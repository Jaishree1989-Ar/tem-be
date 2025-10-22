package com.tem.be.api.service.processors;

import com.tem.be.api.dao.TempATTInvoiceDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempATTInvoice;
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
public class ATTInvoiceProcessor implements InvoiceProcessor<TempATTInvoice> {

    private final TempATTInvoiceDao tempATTInvoiceDao;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return CarrierConstants.ATT;
    }

    @Override
    public List<TempATTInvoice> convertAndEnrichData(List<Map<String, String>> dataRows,
                                                     InvoiceHistory history,
                                                     String filename,
                                                     Map<String, String> departmentMapping) {
        return dataRows.stream()
                .filter(Predicate.not(this::isRowEmpty))
                // Delegate all logic for a single row to our robust processRow method
                .map(row -> processRow(row, history, filename, departmentMapping))
                // Filter out any rows that failed processing (returned null)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Processes a single row: normalizes, converts, enriches, and maps the department.
     * This method contains the complete logic for handling one record.
     *
     * @return A fully processed TempATTInvoice object, or null if a critical error occurs.
     */
    private TempATTInvoice processRow(Map<String, String> row, InvoiceHistory history, String filename, Map<String, String> departmentMapping) {
        try {
            // 1. Normalize keys to ensure consistent field mapping
            Map<String, String> normalizedData = normalizeKeys(row);
            TempATTInvoice tempInvoice = objectMapper.convertValue(normalizedData, TempATTInvoice.class);

            // 2. Apply existing enrichment logic (THIS IS THE RESTORED PART)
            generateInvoiceNumber(tempInvoice);
            calculateReoccurringCharges(tempInvoice);

            // 3. Set standard batch metadata
            tempInvoice.setInvoiceHistory(history);
            tempInvoice.setSourceFilename(filename);
            tempInvoice.setStatus(InvoiceStatus.PENDING_APPROVAL);

            // 4. Apply department mapping logic
            String accountNumber = tempInvoice.getAccountNumber();
            if (accountNumber != null && departmentMapping.containsKey(accountNumber)) {
                tempInvoice.setDepartment(departmentMapping.get(accountNumber));
            } else {
                String fallbackDepartment = normalizedData.get("accountName");
                tempInvoice.setDepartment(fallbackDepartment);
                if (accountNumber != null) {
                    log.warn("No department mapping for AT&T account '{}'. Used fallback: '{}'", accountNumber, fallbackDepartment);
                }
            }

            return tempInvoice;

        } catch (Exception e) {
            log.error("Failed to process an AT&T row for file '{}'. Row data: {}. Error: {}", filename, row, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        Map<String, String> normalizedMap = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (entry.getKey() == null) continue;

            String originalKey = entry.getKey().trim();
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

    @Override
    public void saveTempInvoices(List<TempATTInvoice> tempInvoices) {
        tempATTInvoiceDao.saveAll(tempInvoices);
    }

    // --- Helper methods (can be moved to a shared utility class if they are identical) ---

    private boolean isRowEmpty(Map<String, String> row) {
        return row == null || row.values().stream().allMatch(String::isBlank);
    }

    private void generateInvoiceNumber(TempATTInvoice invoice) {
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
            log.warn("Could not parse Account Number '{}' for AT&T. Skipping invoice number generation.", accountNumberRaw);
        }
    }

    private void calculateReoccurringCharges(TempATTInvoice invoice) {
        try {
            BigDecimal totalCurrent = parseBigDecimal(invoice.getTotalCurrentCharges());
            BigDecimal totalActivity = parseBigDecimal(invoice.getTotalActivitySinceLastBill());
            invoice.setTotalReoccurringCharges(totalCurrent.subtract(totalActivity));
        } catch (NumberFormatException e) {
            log.warn("Could not parse AT&T charge fields for wireless number '{}'. Total Reoccurring Charges will be null.", invoice.getWirelessNumber());
            invoice.setTotalReoccurringCharges(null);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        // Removes $, commas, etc.
        return new BigDecimal(value.replaceAll("[^\\d.-]", ""));
    }
}
