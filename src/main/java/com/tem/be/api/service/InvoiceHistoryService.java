package com.tem.be.api.service;

import com.tem.be.api.dto.ApprovedInvoiceBatchDTO;
import com.tem.be.api.dto.InvoiceBatchReviewDTO;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.model.InvoiceHistory;

import java.util.List;

public interface InvoiceHistoryService {
    InvoiceHistory createInvoiceHistory(InvoiceHistory invoiceHistory);

    List<InvoiceHistory> getAllInvoiceHistories();

    boolean deleteInvoiceHistory(Long id);

    InvoiceBatchReviewDTO getBatchForReview(String batchId);

    void processReviewAction(ReviewActionDTO reviewAction);

    ApprovedInvoiceBatchDTO getApprovedBatchDetails(String batchId);

    void performApproval(ReviewActionDTO reviewAction);

    void performRejection(ReviewActionDTO reviewAction);

    void finalizeAsFailed(ReviewActionDTO reviewAction, String reason);

    /**
     * Marks a specific batch as FAILED and records the reason in a new transaction.
     */
    void markAsFailed(String batchId, String reason);
}
