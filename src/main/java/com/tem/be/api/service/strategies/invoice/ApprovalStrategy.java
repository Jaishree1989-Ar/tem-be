package com.tem.be.api.service.strategies.invoice;

import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempInvoiceBase;

import java.util.List;

/**
 * Strategy interface for handling the review and approval workflow for a specific provider.
 * Each implementation encapsulates the logic for fetching, converting, and cleaning up
 * invoice data for its designated provider.
 */
public interface ApprovalStrategy {

    /**
     * Retrieves the temporary invoices associated with a batch for user review.
     *
     * @param batchId The unique ID of the batch.
     * @return A list of temporary invoice entities.
     */
    List<? extends TempInvoiceBase> getTemporaryInvoicesForReview(String batchId);

    /**
     * Executes the approval logic for a given batch. This includes converting
     * temporary invoices to final invoices, saving them, and cleaning up the
     * temporary records.
     *
     * @param history The parent InvoiceHistory object for the batch being approved.
     */
    void approve(InvoiceHistory history);

    /**
     * Deletes the temporary invoices associated with a rejected batch.
     *
     * @param batchId The unique ID of the batch to reject.
     */
    void reject(String batchId);

    /**
     * Retrieves the final, approved invoices for a given batch.
     *
     * @param batchId The unique ID of the batch.
     * @return A list of final invoice entities.
     */
    List<?> getFinalInvoices(String batchId);

    /**
     * Returns the name of the provider this strategy supports (e.g., "FirstNet", "AT&T Mobility").
     *
     * @return The provider name as a String.
     */
    String getProviderName();
}
