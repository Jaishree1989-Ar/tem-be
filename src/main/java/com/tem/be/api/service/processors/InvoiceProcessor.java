package com.tem.be.api.service.processors;

import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempInvoiceBase;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for processing invoice data for a specific provider.
 * Each implementation will handle the unique mapping, validation, and saving
 * logic for its designated provider (e.g., AT&T, FirstNet).
 *
 * @param <T> The type of the temporary invoice entity (e.g., TempATTInvoice).
 */
public interface InvoiceProcessor<T extends TempInvoiceBase> {

    /**
     * Converts a list of raw data maps (from the parsed file) into a list
     * of provider-specific temporary invoice entities. This includes data enrichment,
     * invoice number generation, and applying business logic.
     *
     * @param dataRows The list of maps, where each map represents a row from the file.
     * @param history  The parent InvoiceHistory record for this batch.
     * @param filename The original name of the uploaded file.
     * @return A list of enriched temporary invoice entities ready for persistence.
     */
    List<T> convertAndEnrichData(List<Map<String, String>> dataRows, InvoiceHistory history, String filename);

    /**
     * Saves a list of provider-specific temporary invoice entities to the database.
     *
     * @param tempInvoices The list of temporary invoices to save.
     */
    void saveTempInvoices(List<T> tempInvoices);

    /**
     * Returns the name of the provider this processor supports (e.g., "FirstNet", "ATT").
     * This name must match the key in the configuration and the 'provider' request parameter.
     *
     * @return The provider name as a String.
     */
    String getProviderName();
}
