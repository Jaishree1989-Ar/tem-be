package com.tem.be.api.service;

import com.tem.be.api.dao.InvoiceHistoryDao;
import com.tem.be.api.dto.ApprovedInvoiceBatchDTO;
import com.tem.be.api.dto.InvoiceBatchReviewDTO;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InvoiceApprovalException;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempInvoiceBase;
import com.tem.be.api.service.strategies.invoice.ApprovalStrategy;
import com.tem.be.api.service.strategies.invoice.ApprovalStrategyFactory;
import lombok.extern.log4j.Log4j2;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.util.List;

@Service
@Log4j2
public class InvoiceHistoryServiceImpl implements InvoiceHistoryService {
    private final InvoiceHistoryDao invoiceHistoryDao;
    private static final String BATCH_NOT_FOUND_ERROR = "Batch not found with ID: ";

    private final ApprovalStrategyFactory approvalStrategyFactory;
    private InvoiceHistoryService self;

    @Autowired
    public InvoiceHistoryServiceImpl(InvoiceHistoryDao invoiceHistoryDao,
                                     ApprovalStrategyFactory approvalStrategyFactory) {
        this.invoiceHistoryDao = invoiceHistoryDao;
        this.approvalStrategyFactory = approvalStrategyFactory;
    }

    @Autowired
    @Lazy
    public void setSelf(InvoiceHistoryService self) {
        this.self = self;
    }

    @Override
    @Transactional
    public InvoiceHistory createInvoiceHistory(InvoiceHistory invoiceHistory) {
        return invoiceHistoryDao.save(invoiceHistory);
    }

    @Override
    public List<InvoiceHistory> getAllInvoiceHistories() {
        return invoiceHistoryDao.findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public boolean deleteInvoiceHistory(Long id) {
        return invoiceHistoryDao.findById(id).map(history -> {
            history.setIsDeleted(true);
            invoiceHistoryDao.save(history);
            return true;
        }).orElse(false);
    }

    /**
     * Retrieves a batch of invoices pending review.
     *
     * @param batchId The unique identifier of the batch.
     * @return An {@link InvoiceBatchReviewDTO} containing the batch metadata and the list of temporary invoices.
     * @throws RuntimeException if no batch is found with the given ID.
     */
    @Override
    public InvoiceBatchReviewDTO getBatchForReview(String batchId) {
        log.info("InvoiceHistoryServiceImpl.getBatchForReview() >> Entry | Batch ID: {}", batchId);

        // Fetch the parent history record along with its associated temporary invoices.
        InvoiceHistory history = invoiceHistoryDao.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException(BATCH_NOT_FOUND_ERROR + batchId));

        // Delegate fetching to the correct strategy
        ApprovalStrategy strategy = approvalStrategyFactory.getStrategy(history.getCarrier());
        List<? extends TempInvoiceBase> tempInvoices = strategy.getTemporaryInvoicesForReview(batchId);


        log.info("InvoiceHistoryServiceImpl.getBatchForReview() >> Exited Successfully | Found {} records for Batch ID: {}", history.getTempInvoices().size(), batchId);
        return new InvoiceBatchReviewDTO(history, tempInvoices);
    }

    /**
     * Processes a review action (APPROVE or REJECT) for a given batch.
     * If approved, it moves data from the temporary table to the final table.
     * If rejected, it simply updates the batch status.
     * In both cases, the temporary data is cleared.
     *
     * @param reviewAction The DTO containing the batch ID and the action to perform.
     * @throws RuntimeException if no batch is found with the given ID.
     */
    @Override
    public void processReviewAction(ReviewActionDTO reviewAction) {
        log.info("Orchestrating review action >> Action: {}, Batch ID: {}", reviewAction.getAction(), reviewAction.getBatchId());

        if (reviewAction.getAction() == ReviewActionDTO.ActionType.APPROVE) {
            try {
                // Delegate the entire approval workflow to a single atomic transaction.
                self.performApproval(reviewAction);
                log.info("Successfully approved batch {}", reviewAction.getBatchId());
            } catch (Exception e) {
                // The performApproval transaction has already been rolled back by Spring.
                log.error("Approval transaction failed for batch {}. Finalizing as FAILED.", reviewAction.getBatchId(), e);

                String failureReason;
                if (e.getCause() instanceof ConstraintViolationException) {
                    failureReason = "The uploaded file contains duplicate invoice entries.";
                } else {
                    failureReason = "An unexpected error occurred during approval.";
                }

                // Call the dedicated failure method in a NEW transaction.
                // This guarantees the FAILED status, review details, and cleanup are saved.
                self.finalizeAsFailed(reviewAction, "Approval failed: " + failureReason);

                // Re-throw so the controller/client knows the operation ultimately failed.
                throw new InvoiceApprovalException("Failed to process approval for batch " + reviewAction.getBatchId(), e);
            }
        } else if (reviewAction.getAction() == ReviewActionDTO.ActionType.REJECT) {
            // Delegate rejection to its own atomic transaction.
            self.performRejection(reviewAction);
            log.info("Successfully rejected batch {}", reviewAction.getBatchId());
        }
    }

    /**
     * Performs an atomic approval workflow for a given invoice batch.
     *
     * @param reviewAction The Data Transfer Object (DTO) containing the batch ID and reviewer details.
     * @throws RuntimeException if the batch is not found.
     */
    @Override
    @Transactional
    public void performApproval(ReviewActionDTO reviewAction) {
        log.info("Starting transactional approval for batch {}", reviewAction.getBatchId());
        InvoiceHistory history = invoiceHistoryDao.findByBatchId(reviewAction.getBatchId())
                .orElseThrow(() -> new RuntimeException(BATCH_NOT_FOUND_ERROR + reviewAction.getBatchId()));

        // Delegate approval logic to the correct strategy
        ApprovalStrategy strategy = approvalStrategyFactory.getStrategy(history.getCarrier());
        strategy.approve(history); // This handles conversion, saving, and cleanup

        // Update history status
        history.setReviewedBy(reviewAction.getReviewedBy());
        history.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        history.setStatus(InvoiceStatus.APPROVED);
        history.setRejectionReason(null);
        invoiceHistoryDao.save(history);
        log.info("Approval transaction complete for batch {}", reviewAction.getBatchId());
    }

    @Override
    @Transactional
    public void performRejection(ReviewActionDTO reviewAction) {
        log.info("Starting transactional rejection for batch {}", reviewAction.getBatchId());
        InvoiceHistory history = invoiceHistoryDao.findByBatchId(reviewAction.getBatchId())
                .orElseThrow(() -> new RuntimeException(BATCH_NOT_FOUND_ERROR + reviewAction.getBatchId()));

        // Delegate rejection cleanup to the correct strategy
        ApprovalStrategy strategy = approvalStrategyFactory.getStrategy(history.getCarrier());
        strategy.reject(history.getBatchId());

        // Update history status
        history.setReviewedBy(reviewAction.getReviewedBy());
        history.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        history.setStatus(InvoiceStatus.REJECTED);
        history.setRejectionReason(reviewAction.getRejectionReason());
        invoiceHistoryDao.save(history);
        log.info("Rejection transaction complete for batch {}", reviewAction.getBatchId());
    }

    /**
     * Finalizes a batch as FAILED in a new, independent transaction.
     *
     * @param reviewAction The DTO containing the batch ID and reviewer details.
     * @param reason       A descriptive reason for the failure.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeAsFailed(ReviewActionDTO reviewAction, String reason) {
        log.error("Finalizing batch {} as FAILED. Reason: {}", reviewAction.getBatchId(), reason);

        invoiceHistoryDao.findByBatchId(reviewAction.getBatchId()).ifPresent(history -> {
            // Set review details
            history.setReviewedBy(reviewAction.getReviewedBy());
            history.setReviewedAt(new Timestamp(System.currentTimeMillis()));

            // Finalize history state for failure
            history.setStatus(InvoiceStatus.FAILED);
            String truncatedReason = (reason != null && reason.length() > 1024) ? reason.substring(0, 1023) : reason;
            history.setRejectionReason(truncatedReason);
            history.getTempInvoices().clear(); // Clean up temp data

            invoiceHistoryDao.save(history); // Commit all changes
            log.info("Successfully finalized batch {} with FAILED status.", reviewAction.getBatchId());
        });
    }

    /**
     * Retrieves the details of an approved batch and its associated final invoices.
     *
     * @param batchId The unique identifier of the batch.
     * @return An {@link ApprovedInvoiceBatchDTO} containing batch details and final invoices.
     * @throws EntityNotFoundException if no batch is found with the given ID.
     * @throws IllegalStateException   if the found batch is not in an APPROVED state.
     */
    @Override
    public ApprovedInvoiceBatchDTO getApprovedBatchDetails(String batchId) {
        log.info("Getting approved batch details for: {}", batchId);
        InvoiceHistory history = invoiceHistoryDao.findByBatchId(batchId)
                .orElseThrow(() -> new EntityNotFoundException(BATCH_NOT_FOUND_ERROR + batchId));

        if (history.getStatus() != InvoiceStatus.APPROVED) {
            throw new IllegalStateException("Batch is not approved. Current status: " + history.getStatus());
        }

        // Delegate fetching of final invoices to the correct strategy
        ApprovalStrategy strategy = approvalStrategyFactory.getStrategy(history.getCarrier());
        List<?> finalInvoices = strategy.getFinalInvoices(batchId);

        log.info("Found {} approved invoices for Batch ID: {}", finalInvoices.size(), batchId);
        return new ApprovedInvoiceBatchDTO(history, finalInvoices);
    }

    /**
     * Marks a batch as FAILED and records the reason.
     * This method runs in a NEW transaction to ensure that even if the calling
     * process's transaction is rolled back, this status update is committed to the database.
     * This is crucial for auditing failed uploads.
     *
     * @param batchId The unique ID of the batch to update.
     * @param reason  The error message or reason for the failure.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(String batchId, String reason) {
        log.error("Marking batch {} as FAILED. Reason: {}", batchId, reason);
        invoiceHistoryDao.findByBatchId(batchId).ifPresent(history -> {
            history.setStatus(InvoiceStatus.FAILED);
            // Truncate reason if it's too long for the database column
            String truncatedReason = (reason != null && reason.length() > 1024) ? reason.substring(0, 1023) : reason;
            history.setRejectionReason(truncatedReason);
            invoiceHistoryDao.save(history);
            log.info("Successfully updated batch {} to FAILED status.", batchId);
        });
    }
}
