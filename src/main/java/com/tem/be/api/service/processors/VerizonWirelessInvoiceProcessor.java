package com.tem.be.api.service.processors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dao.TempVerizonWirelessInvoiceDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempVerizonWirelessInvoice;
import com.tem.be.api.utils.CarrierConstants;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log4j2
@Component
@AllArgsConstructor
public class VerizonWirelessInvoiceProcessor implements InvoiceProcessor<TempVerizonWirelessInvoice> {

    private final TempVerizonWirelessInvoiceDao tempVerizonWirelessInvoiceDao;
    private final ObjectMapper objectMapper;

    // A static set of keys that correspond to numeric fields for efficient lookup.
    private static final Set<String> NUMERIC_FIELD_KEYS = new HashSet<>(Arrays.asList(
            "accountChargesAndCredits", "adjustments", "balanceForward", "equipmentCharges",
            "lateFee", "monthlyCharges", "payments", "previousBalance", "surchargesAndOccs",
            "taxesGovSurchargesAndFees", "thirdPartyChargesToAccount",
            "thirdPartyChargesToLines", "totalAmountDue", "totalCurrentCharges",
            "usageAndPurchaseCharges", "usageChargesData", "usageChargesPurchases",
            "usageChargesRoaming", "usageChargesVoice"
    ));

    @Override
    public String getProviderName() {
        return CarrierConstants.VERIZON_WIRELESS;
    }

    @Override
    public List<TempVerizonWirelessInvoice> convertAndEnrichData(List<Map<String, String>> dataRows, InvoiceHistory history, String filename, Map<String, String> departmentMapping) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return dataRows.stream()
                .filter(Predicate.not(this::isRowEmpty))
                .map(row -> processRow(row, history, filename, departmentMapping))
                .filter(Objects::nonNull)
                .toList();
    }

    private TempVerizonWirelessInvoice processRow(Map<String, String> row, InvoiceHistory history, String filename, Map<String, String> departmentMapping) {
        try {
            // Step 1: Normalize the keys of the map (e.g., "Previous Balance" -> "previousBalance")
            Map<String, String> cleanedData = normalizeKeys(row);

            // Step 2: Sanitize the numeric string values *in the map* before conversion.
            // This is the critical fix. We remove commas so ObjectMapper can parse them.
            sanitizeNumericStringsInMap(cleanedData);

            // Step 3: Now that the map is clean, convert it to the object. This will no longer fail.
            TempVerizonWirelessInvoice tempInvoice = objectMapper.convertValue(cleanedData, TempVerizonWirelessInvoice.class);

            calculateReoccurringCharges(tempInvoice);

            // Step 4: Perform any additional enrichment.
            parseBillPeriod(tempInvoice);

            // The invoice number also needs to have its commas removed.
            tempInvoice.setInvoiceNumber(removeCommas(cleanedData.get("invoiceNumber")));

            tempInvoice.setInvoiceHistory(history);
            tempInvoice.setSourceFilename(filename);
            tempInvoice.setStatus(InvoiceStatus.PENDING_APPROVAL);

            // 5. Apply department mapping logic
            String accountNumber = tempInvoice.getAccountNumber();
            if (accountNumber != null && departmentMapping.containsKey(accountNumber)) {
                tempInvoice.setDepartment(departmentMapping.get(accountNumber));
            } else if (accountNumber != null) {
                log.warn("No department mapping found for Verizon account '{}'. Using default value from 'Bill address level 1': '{}'",
                        accountNumber, tempInvoice.getDepartment());
            }

            return tempInvoice;
        } catch (Exception e) {
            log.error("Failed to process a Verizon Wireless row for file '{}'. Row data: {}. Error: {}", filename, row, e.getMessage(), e);
            // Return null so this faulty row is filtered out and doesn't stop the whole file processing.
            return null;
        }
    }

    /**
     * Pre-processes the data map to remove commas from any string value
     * that corresponds to a known numeric field. This must be done *before*
     * ObjectMapper attempts to convert the map to a Java object.
     *
     * @param data The map with normalized keys and raw string values.
     */
    private void sanitizeNumericStringsInMap(Map<String, String> data) {
        for (String key : NUMERIC_FIELD_KEYS) {
            // computeIfPresent will only execute the lambda if the key is present AND the existing value is non-null.
            data.computeIfPresent(key, (k, existingValue) -> removeCommas(existingValue));
        }
    }

    /**
     * Sets the total recurring charges for a Verizon invoice.
     * For Verizon, this is a direct mapping from the 'Monthly charges' field.
     */
    private void calculateReoccurringCharges(TempVerizonWirelessInvoice invoice) {
        try {
            // The 'monthlyCharges' field is already populated and cleaned by the time this is called.
            BigDecimal monthlyCharges = invoice.getMonthlyCharges();
            if (monthlyCharges != null) {
                invoice.setTotalReoccurringCharges(monthlyCharges);
            } else {
                // Set to zero if the source field is null to avoid NullPointerExceptions
                invoice.setTotalReoccurringCharges(BigDecimal.ZERO);
                log.warn("'Monthly charges' field was null for Account Number '{}'. Setting recurring charges to zero.", invoice.getAccountNumber());
            }
        } catch (Exception e) {
            log.warn("Could not set Total Reoccurring Charges for account '{}'. It will be null.", invoice.getAccountNumber(), e);
            invoice.setTotalReoccurringCharges(null);
        }
    }

    private void parseBillPeriod(TempVerizonWirelessInvoice invoice) {
        String billPeriodStr = invoice.getBillPeriod();
        if (billPeriodStr == null || billPeriodStr.isBlank() || !billPeriodStr.contains(" - ")) return;
        try {
            String[] dates = billPeriodStr.split(" - ");
            SimpleDateFormat parser = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
            invoice.setBillPeriodStart(parser.parse(dates[0]));
            invoice.setBillPeriodEnd(parser.parse(dates[1]));
        } catch (ParseException e) {
            log.warn("Could not parse Bill Period '{}'. Start/End dates will be null.", billPeriodStr);
        }
    }

    private String removeCommas(String value) {
        if (value == null || value.isBlank()) return value;
        return value.replace(",", "").trim();
    }

    private boolean isRowEmpty(Map<String, String> row) {
        return row == null || row.values().stream().allMatch(val -> val == null || val.isBlank());
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        Map<String, String> normalizedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (entry.getKey() == null) continue;
            String camelCaseKey = toCamelCase(entry.getKey().trim());
            normalizedMap.put(camelCaseKey, entry.getValue());
        }
        return normalizedMap;
    }

    private String toCamelCase(String s) {
        // This regex handles various separators like spaces, commas, and parentheses.
        String[] parts = s.split("[^a-zA-Z0-9]+");
        StringBuilder camelCaseString = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            if (i == 0) {
                camelCaseString.append(part.toLowerCase());
            } else {
                camelCaseString.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
            }
        }
        return camelCaseString.toString();
    }

    @Override
    public void saveTempInvoices(List<TempVerizonWirelessInvoice> tempInvoices) {
        if (tempInvoices != null && !tempInvoices.isEmpty()) {
            tempVerizonWirelessInvoiceDao.saveAll(tempInvoices);
        }
    }
}
