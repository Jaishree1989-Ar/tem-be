package com.tem.be.api.service.processors;

import com.tem.be.api.dao.TempATTInvoiceDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempATTInvoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Log4j2
@Component
public class ATTInvoiceProcessor implements InvoiceProcessor<TempATTInvoice> {

    private final TempATTInvoiceDao tempATTInvoiceDao;
    private final ObjectMapper objectMapper;

    @Autowired
    public ATTInvoiceProcessor(TempATTInvoiceDao tempATTInvoiceDao, ObjectMapper objectMapper) {
        this.tempATTInvoiceDao = tempATTInvoiceDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "AT&T Mobility";
    }

    @Override
    public List<TempATTInvoice> convertAndEnrichData(List<Map<String, String>> dataRows, InvoiceHistory history, String filename) {
        return dataRows.stream()
                .filter(Predicate.not(this::isRowEmpty))
                .map(row -> processRow(row, history, filename))
                .toList();
    }

    private TempATTInvoice processRow(Map<String, String> row, InvoiceHistory history, String filename) {
        // The original 'row' map with original headers is now used directly.
        // The @JsonProperty annotations on the TempATTInvoice class do all the work.
        TempATTInvoice tempInvoice = objectMapper.convertValue(row, TempATTInvoice.class);

        // Continue with enrichment logic
        generateInvoiceNumber(tempInvoice);
        calculateReoccurringCharges(tempInvoice);

        tempInvoice.setInvoiceHistory(history);
        tempInvoice.setSourceFilename(filename);
        tempInvoice.setStatus(InvoiceStatus.PENDING_APPROVAL);

        return tempInvoice;
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
