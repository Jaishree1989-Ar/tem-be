package com.tem.be.api.service.processors;

import com.tem.be.api.dao.TempFirstNetInvoiceDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempFirstNetInvoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.utils.CarrierConstants;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@Log4j2
@Component
@AllArgsConstructor
public class FirstNetInvoiceProcessor implements InvoiceProcessor<TempFirstNetInvoice> {

    private final TempFirstNetInvoiceDao tempFirstNetInvoiceDao;
    private final ObjectMapper objectMapper;

    private static final String NORMALIZED_KEY_DEPARTMENT = "department";
    private static final String NORMALIZED_KEY_BILLING_ACCOUNT_NAME = "billingAccountName";
    private static final String NORMALIZED_KEY_UDL2 = "udl2";

    @Override
    public String getProviderName() {
        return CarrierConstants.FIRSTNET;
    }

    @Override
    public List<TempFirstNetInvoice> convertAndEnrichData(List<Map<String, String>> dataRows, InvoiceHistory history, String filename, Map<String, String> departmentMapping) {
        return dataRows.stream()
                .filter(Predicate.not(this::isRowEmpty))
                // Each row is now processed by our enhanced helper method
                .map(row -> processRow(row, history, filename, departmentMapping))
                // This crucial step filters out any rows that failed processing and returned null
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Processes a single row of data, converting it to a TempFirstNetInvoice entity,
     * applying enrichment logic, and handling department mapping.
     *
     * @return A fully processed TempFirstNetInvoice object, or null if processing fails.
     */
    private TempFirstNetInvoice processRow(Map<String, String> row, InvoiceHistory history, String filename, Map<String, String> departmentMapping) {
        try {
            // 1. Normalize keys before mapping to the object
            Map<String, String> cleanedData = normalizeKeys(row);
            applyCustomTransformations(cleanedData);
            TempFirstNetInvoice tempInvoice = objectMapper.convertValue(cleanedData, TempFirstNetInvoice.class);

            // 2. Apply existing enrichment logic (invoice number, charges, etc.)
            generateInvoiceNumber(tempInvoice);
            calculateReoccurringCharges(tempInvoice);

            // 3. Set batch-level metadata
            tempInvoice.setInvoiceHistory(history);
            tempInvoice.setSourceFilename(filename);
            tempInvoice.setStatus(InvoiceStatus.PENDING_APPROVAL);

            // 4. Apply the new department mapping logic
            String accountNumber = tempInvoice.getAccountNumber();
            if (accountNumber != null && departmentMapping.containsKey(accountNumber)) {
                tempInvoice.setDepartment(departmentMapping.get(accountNumber));
            } else if (accountNumber != null) {
                log.warn("No department mapping found for FirstNet account '{}'. Department will be null.", accountNumber);
            }

            return tempInvoice;
        } catch (Exception e) {
            log.error("Failed to process a FirstNet row for file '{}'. Row data: {}. Error: {}", filename, row, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        Map<String, String> normalizedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (entry.getKey() == null) continue;

            String originalKey = entry.getKey().trim();
            // This regex-based approach is robust for converting "Header Name" to "headerName"
            String[] parts = originalKey.replaceAll("[^a-zA-Z0-9 ]", "").toLowerCase().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) continue;

            StringBuilder camelCaseKey = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    camelCaseKey.append(Character.toUpperCase(parts[i].charAt(0)))
                            .append(parts[i].substring(1));
                }
            }
            normalizedMap.put(camelCaseKey.toString(), entry.getValue() != null ? entry.getValue().trim() : null);
        }
        return normalizedMap;
    }

    /**
     * Applies custom business logic transformations to the data map.
     * This method modifies the map in place.
     * - 'department' is moved to 'division'.
     * - 'billingAccountName' becomes the new 'department'.
     * - 'udl2' is moved to 'visCode'.
     *
     * @param cleanedRowData The map with normalized keys.
     */
    private void applyCustomTransformations(Map<String, String> cleanedRowData) {
        // Department/Division Logic
        String originalDepartmentValue = cleanedRowData.get(NORMALIZED_KEY_DEPARTMENT);
        cleanedRowData.put("division", originalDepartmentValue); // This key must match the Java field name

        String newDepartmentValue = cleanedRowData.get(NORMALIZED_KEY_BILLING_ACCOUNT_NAME);
        cleanedRowData.put(NORMALIZED_KEY_DEPARTMENT, newDepartmentValue);

        // UDL 2 to VIS Code Logic
        if (cleanedRowData.containsKey(NORMALIZED_KEY_UDL2)) {
            String udl2Value = cleanedRowData.remove(NORMALIZED_KEY_UDL2);
            cleanedRowData.put("visCode", udl2Value); // This key must match the Java field name
        }
    }

    @Override
    public void saveTempInvoices(List<TempFirstNetInvoice> tempInvoices) {
        tempFirstNetInvoiceDao.saveAll(tempInvoices);
    }

    // --- Helper methods moved from the service ---

    private boolean isRowEmpty(Map<String, String> row) {
        return row == null || row.values().stream().allMatch(String::isBlank);
    }

    private void generateInvoiceNumber(TempFirstNetInvoice invoice) {
        String accountNumberRaw = invoice.getAccountNumber();
        if (accountNumberRaw == null || accountNumberRaw.isBlank()) return;

        try {
            String fullAccountNumber = new BigDecimal(accountNumberRaw).toPlainString();
            invoice.setAccountNumber(fullAccountNumber);
            if (invoice.getInvoiceDate() == null) return;

            LocalDate invoiceDate = invoice.getInvoiceDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String invoiceIdSuffix = invoiceDate.format(DateTimeFormatter.ofPattern("MM'09'yyyy"));
            invoice.setInvoiceNumber(fullAccountNumber + "X" + invoiceIdSuffix);
        } catch (NumberFormatException e) {
            log.warn("Could not parse Account Number '{}'. Skipping invoice number generation.", accountNumberRaw);
        }
    }

    private void calculateReoccurringCharges(TempFirstNetInvoice invoice) {
        try {
            BigDecimal totalCurrent = parseBigDecimal(invoice.getTotalCurrentCharges());
            BigDecimal totalActivity = parseBigDecimal(invoice.getTotalActivitySinceLastBill());
            invoice.setTotalReoccurringCharges(totalCurrent.subtract(totalActivity));
        } catch (NumberFormatException e) {
            log.warn("Could not parse charge fields for wireless number '{}'. Total Reoccurring Charges will be null.", invoice.getWirelessNumber());
            invoice.setTotalReoccurringCharges(null);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replaceAll("[^\\d.-]", ""));
    }
}
