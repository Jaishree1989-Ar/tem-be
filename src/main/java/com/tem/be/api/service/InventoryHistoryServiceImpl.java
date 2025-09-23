package com.tem.be.api.service;

import com.tem.be.api.dao.inventory.InventoryHistoryDao;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.dto.inventory.ApprovedInventoryBatchDTO;
import com.tem.be.api.dto.inventory.InventoryBatchReviewDTO;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InventoryApprovalException;
import com.tem.be.api.model.*;
import com.tem.be.api.service.strategies.inventory.InventoryApprovalStrategy;
import com.tem.be.api.service.strategies.inventory.InventoryApprovalStrategyFactory;
import lombok.extern.log4j.Log4j2;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.util.List;

@Service
@Transactional
@Log4j2
public class InventoryHistoryServiceImpl implements InventoryHistoryService {

    private final InventoryHistoryDao inventoryHistoryDao;
    private final InventoryApprovalStrategyFactory strategyFactory;
    public static final String BATCH_NOT_FOUND_ERROR = "Inventory batch not found with ID: ";

    // Self-injection to ensure transactional proxy is invoked
    private InventoryHistoryService self;

    @Autowired
    public InventoryHistoryServiceImpl(InventoryHistoryDao inventoryHistoryDao,
                                       InventoryApprovalStrategyFactory strategyFactory) {
        this.inventoryHistoryDao = inventoryHistoryDao;
        this.strategyFactory = strategyFactory;
    }

    @Autowired
    @Lazy
    public void setSelf(InventoryHistoryService self) {
        this.self = self;
    }

    /**
     * Creates and persists a new InventoryHistory record.
     *
     * @param history The {@link InventoryHistory} object to save.
     * @return The saved InventoryHistory entity with its generated ID.
     */
    @Override
    public InventoryHistory createInventoryHistory(InventoryHistory history) {
        log.info("InventoryHistoryServiceImpl.createInventoryHistory >> Entry | Batch ID: {}", history.getBatchId());
        InventoryHistory savedHistory = inventoryHistoryDao.save(history);
        log.info("InventoryHistoryServiceImpl.createInventoryHistory >> Exited Successfully | Saved InventoryHistory with ID: {}", savedHistory.getInventoryHistoryId());
        return savedHistory;
    }

    /**
     * Retrieves an inventory batch and its associated temporary records for review.
     *
     * @param batchId The unique identifier of the batch.
     * @return An {@link InventoryBatchReviewDTO} containing the batch metadata and staged data.
     */
    @Override
    public InventoryBatchReviewDTO getBatchForReview(String batchId) {
        log.info("InventoryHistoryServiceImpl.getBatchForReview >> Entry | Batch ID: {}", batchId);
        InventoryHistory history = inventoryHistoryDao.findByBatchId(batchId)
                .orElseThrow(() -> new EntityNotFoundException(BATCH_NOT_FOUND_ERROR + batchId));

        // Delegate fetching to the correct strategy
        InventoryApprovalStrategy strategy = strategyFactory.getStrategy(history.getCarrier()); // Assuming a 'provider' field
        List<? extends TempInventoryBase> tempInventories = strategy.getTemporaryInventoriesForReview(batchId);

        log.info("InventoryHistoryServiceImpl.getBatchForReview >> Exited Successfully | Found {} records for Batch ID: {}", tempInventories.size(), batchId);
        return new InventoryBatchReviewDTO(history, tempInventories);
    }

    /**
     * Processes a review action (APPROVE or REJECT) for a given inventory batch.
     *
     * @param reviewAction The DTO containing the batch ID and the action to perform.
     */
    @Override
    public void processReviewAction(ReviewActionDTO reviewAction) {
        log.info("Orchestrating inventory review action >> Action: {}, Batch ID: {}", reviewAction.getAction(), reviewAction.getBatchId());

        if (reviewAction.getAction() == ReviewActionDTO.ActionType.APPROVE) {
            try {
                self.performApproval(reviewAction);
                log.info("Successfully approved inventory batch {}", reviewAction.getBatchId());
            } catch (Exception e) {
                log.error("Approval transaction failed for inventory batch {}. Finalizing as FAILED.", reviewAction.getBatchId(), e);

                String failureReason = (e.getCause() instanceof ConstraintViolationException)
                        ? "The uploaded file contains duplicate inventory entries or violates a database constraint."
                        : "An unexpected error occurred during approval process.";

                self.asynchronouslyFinalizeAsFailed(reviewAction, "Approval failed: " + failureReason);
                throw new InventoryApprovalException("Failed to process approval for batch " + reviewAction.getBatchId(), e);
            }
        } else if (reviewAction.getAction() == ReviewActionDTO.ActionType.REJECT) {
            self.performRejection(reviewAction);
            log.info("Successfully rejected inventory batch {}", reviewAction.getBatchId());
        }
    }

    /**
     * Performs the approval workflow within a single, atomic transaction.
     */
    @Override
    @Transactional
    public void performApproval(ReviewActionDTO reviewAction) {
        log.info("Starting transactional approval for inventory batch {}", reviewAction.getBatchId());

        InventoryHistory history = inventoryHistoryDao.findByBatchId(reviewAction.getBatchId())
                .orElseThrow(() -> new EntityNotFoundException(BATCH_NOT_FOUND_ERROR + reviewAction.getBatchId()));

        // Delegate approval logic to the correct strategy
        InventoryApprovalStrategy strategy = strategyFactory.getStrategy(history.getCarrier());
        strategy.approve(history); // This handles conversion, saving, and cleanup

        // Update history status
        history.setReviewedBy(reviewAction.getReviewedBy());
        history.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        history.setStatus(InvoiceStatus.APPROVED);
        history.setRejectionReason(null);
        inventoryHistoryDao.save(history);
        log.info("Approval transaction complete for batch {}", reviewAction.getBatchId());
    }

    /**
     * Performs the rejection workflow within a single, atomic transaction.
     */
    @Override
    @Transactional
    public void performRejection(ReviewActionDTO reviewAction) {
        log.info("Starting transactional rejection for inventory batch {}", reviewAction.getBatchId());

        InventoryHistory history = inventoryHistoryDao.findByBatchId(reviewAction.getBatchId())
                .orElseThrow(() -> new EntityNotFoundException(BATCH_NOT_FOUND_ERROR + reviewAction.getBatchId()));

        // Delegate rejection cleanup to the correct strategy
        InventoryApprovalStrategy strategy = strategyFactory.getStrategy(history.getCarrier());
        strategy.reject(history.getBatchId());

        // Update history status
        history.setReviewedBy(reviewAction.getReviewedBy());
        history.setReviewedAt(new Timestamp(System.currentTimeMillis()));
        history.setStatus(InvoiceStatus.REJECTED);
        history.setRejectionReason(reviewAction.getRejectionReason());
        inventoryHistoryDao.save(history);
        log.info("Rejection transaction complete for batch {}", reviewAction.getBatchId());
    }

    @Async
    public void asynchronouslyFinalizeAsFailed(ReviewActionDTO reviewAction, String reason) {
        log.info("Scheduling asynchronous finalization for batch {}", reviewAction.getBatchId());
        try {
            // The self-proxy is essential here for transactions and async to work
            self.finalizeAsFailed(reviewAction, reason);
        } catch (Exception e) {
            log.error("Asynchronous finalization for batch {} also failed.", reviewAction.getBatchId(), e);
            // At this point, you might send an alert to an admin, as the batch is now in an inconsistent state.
        }
    }

    /**
     * Finalizes a batch as FAILED in a new, independent transaction.
     * This is crucial for updating the batch status even if the primary transaction was rolled back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeAsFailed(ReviewActionDTO reviewAction, String reason) {
        log.error("Finalizing batch {} as FAILED. Reason: {}", reviewAction.getBatchId(), reason);

        inventoryHistoryDao.findByBatchId(reviewAction.getBatchId()).ifPresent(history -> {
            // Set review details
            history.setReviewedBy(reviewAction.getReviewedBy());
            history.setReviewedAt(new Timestamp(System.currentTimeMillis()));

            // Finalize history state for failure
            history.setStatus(InvoiceStatus.FAILED);
            String truncatedReason = (reason != null && reason.length() > 1024) ? reason.substring(0, 1023) : reason;
            history.setRejectionReason(truncatedReason);
            history.getTempInventories().clear(); // Clean up temp data

            inventoryHistoryDao.save(history); // Commit all changes
            log.info("Successfully finalized batch {} with FAILED status.", reviewAction.getBatchId());
        });
    }

    /**
     * Retrieves the details of an approved inventory batch and its final records.
     *
     * @param batchId The unique identifier of the batch.
     * @return An {@link ApprovedInventoryBatchDTO} containing batch details and final inventory records.
     * @throws javax.persistence.EntityNotFoundException if the batch is not found.
     * @throws IllegalStateException                     if the batch is not in an APPROVED state.
     */
    @Override
    public ApprovedInventoryBatchDTO getApprovedBatchDetails(String batchId) {
        log.info("InventoryHistoryServiceImpl.getApprovedBatchDetails >> Entry | Batch ID: {}", batchId);
        InventoryHistory history = inventoryHistoryDao.findByBatchId(batchId)
                .orElseThrow(() -> new EntityNotFoundException(BATCH_NOT_FOUND_ERROR + batchId));

        if (history.getStatus() != InvoiceStatus.APPROVED) {
            log.warn("Batch {} is not in APPROVED state. Current status: {}", batchId, history.getStatus());
            throw new IllegalStateException("Cannot retrieve final records for a batch that is not approved. Status: " + history.getStatus());
        }

        // Delegate fetching of final inventories to the correct strategy
        InventoryApprovalStrategy strategy = strategyFactory.getStrategy(history.getCarrier());
        List<? extends Inventoryable> finalInventories = strategy.getFinalInventories(batchId);

        log.info("Found {} approved inventory records for Batch ID: {}", finalInventories.size(), batchId);
        return new ApprovedInventoryBatchDTO(history, finalInventories);
    }

    /**
     * Marks a specific inventory batch as FAILED in a new transaction.
     *
     * @param batchId The unique ID of the batch to update.
     * @param reason  The error message or reason for the failure.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(String batchId, String reason) {
        log.info("InventoryHistoryServiceImpl.markAsFailed >> Entry | Marking batch {} as FAILED. Reason: {}", batchId, reason);
        inventoryHistoryDao.findByBatchId(batchId).ifPresent(history -> {
            history.setStatus(InvoiceStatus.FAILED);
            String truncatedReason = (reason != null && reason.length() > 512) ? reason.substring(0, 511) : reason;
            history.setRejectionReason(truncatedReason);
            inventoryHistoryDao.save(history);
            log.info("InventoryHistoryServiceImpl.markAsFailed >> Exited Successfully | Updated batch {} to FAILED status.", batchId);
        });
    }

    /**
     * Retrieves all non-deleted inventory history records, ordered by creation date descending.
     *
     * @return A list of {@link InventoryHistory} records.
     */
    @Override
    public List<InventoryHistory> getAllInventoryHistories() {
        return inventoryHistoryDao.findAllByOrderByCreatedAtDesc();
    }
}