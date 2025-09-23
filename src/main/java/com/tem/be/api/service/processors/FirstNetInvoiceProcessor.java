package com.tem.be.api.service.processors;

import com.tem.be.api.dao.TempFirstNetInvoiceDao;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempFirstNetInvoice;
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
import java.util.stream.Collectors;

@Log4j2
@Component
public class FirstNetInvoiceProcessor implements InvoiceProcessor<TempFirstNetInvoice> {

    private final TempFirstNetInvoiceDao tempFirstNetInvoiceDao;
    private final ObjectMapper objectMapper;

    @Autowired
    public FirstNetInvoiceProcessor(TempFirstNetInvoiceDao tempFirstNetInvoiceDao, ObjectMapper objectMapper) {
        this.tempFirstNetInvoiceDao = tempFirstNetInvoiceDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "FirstNet";
    }

    @Override
    public List<TempFirstNetInvoice> convertAndEnrichData(List<Map<String, String>> dataRows, InvoiceHistory history, String filename) {
        return dataRows.stream()
                .filter(Predicate.not(this::isRowEmpty))
                .map(row -> processRow(row, history, filename))
                .toList();
    }

    private TempFirstNetInvoice processRow(Map<String, String> row, InvoiceHistory history, String filename) {
        // The annotations on TempFirstNetInvoice now handle ALL mapping, including the custom logic.
        // No more manual map manipulation is needed.
        TempFirstNetInvoice tempInvoice = objectMapper.convertValue(row, TempFirstNetInvoice.class);

        // Enrichment logic remains the same
        generateInvoiceNumber(tempInvoice);
        calculateReoccurringCharges(tempInvoice);

        tempInvoice.setInvoiceHistory(history);
        tempInvoice.setSourceFilename(filename);
        tempInvoice.setStatus(InvoiceStatus.PENDING_APPROVAL);

        return tempInvoice;
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
