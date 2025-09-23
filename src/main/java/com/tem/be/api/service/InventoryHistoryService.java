package com.tem.be.api.service;

import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.dto.inventory.ApprovedInventoryBatchDTO;
import com.tem.be.api.dto.inventory.InventoryBatchReviewDTO;
import com.tem.be.api.model.InventoryHistory;

import java.util.List;

public interface InventoryHistoryService {
    InventoryHistory createInventoryHistory(InventoryHistory history);

    InventoryBatchReviewDTO getBatchForReview(String batchId);

    void processReviewAction(ReviewActionDTO reviewAction);

    void performApproval(ReviewActionDTO reviewAction);

    void performRejection(ReviewActionDTO reviewAction);

    void asynchronouslyFinalizeAsFailed(ReviewActionDTO reviewAction, String reason);

    void finalizeAsFailed(ReviewActionDTO reviewAction, String reason);

    ApprovedInventoryBatchDTO getApprovedBatchDetails(String batchId);

    void markAsFailed(String batchId, String reason);

    List<InventoryHistory> getAllInventoryHistories();
}
