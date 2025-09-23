package com.tem.be.api.controller;

import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.dto.inventory.ApprovedInventoryBatchDTO;
import com.tem.be.api.dto.inventory.InventoryBatchReviewDTO;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.service.InventoryHistoryService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/inventory-history")
public class InventoryHistoryController {

    private final InventoryHistoryService inventoryHistoryService;

    @Autowired
    public InventoryHistoryController(InventoryHistoryService inventoryHistoryService) {
        this.inventoryHistoryService = inventoryHistoryService;
    }

    /**
     * Retrieves all inventory history records, ordered by creation date in descending order.
     *
     * @return A {@link ResponseEntity} containing an {@link ApiResponse} with a list of {@link InventoryHistory} records.
     */
    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<InventoryHistory>>> getAllInventoryHistories() {
        log.info("InventoryHistoryController.getAllInventoryHistories >> Entry");
        List<InventoryHistory> histories = inventoryHistoryService.getAllInventoryHistories();

        ApiResponse<List<InventoryHistory>> response = new ApiResponse<>(
                RestConstants.SUCCESS_CODE,
                RestConstants.SUCCESS_STRING,
                histories
        );
        log.info("InventoryHistoryController.getAllInventoryHistories() >> Retrieved Successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a batch of staged inventory data for user review before approval.
     * This endpoint returns the batch metadata along with all the temporary inventory records associated with it.
     *
     * @param batchId The unique identifier of the batch to be reviewed.
     * @return A {@link ResponseEntity} containing an {@link ApiResponse} with the {@link InventoryBatchReviewDTO}.
     * The DTO includes batch details and a list of temporary inventory records.
     */
    @GetMapping("/review/{batchId}")
    public ResponseEntity<ApiResponse<InventoryBatchReviewDTO>> getBatchForReview(@PathVariable String batchId) {
        log.info("InventoryHistoryController.getBatchForReview >> Entry | Batch ID: {}", batchId);

        InventoryBatchReviewDTO reviewData = inventoryHistoryService.getBatchForReview(batchId);
        ApiResponse<InventoryBatchReviewDTO> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, reviewData);

        log.info("InventoryHistoryController.getBatchForReview >> Exited Successfully | Found {} records for Batch ID: {}", reviewData.getInventoryRecords().size(), batchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Processes a user action (APPROVE or REJECT) for a batch of staged inventory records.
     * If approved, the data is moved from the temporary table to the final production table.
     * If rejected, the data is discarded, and the batch status is updated accordingly.
     *
     * @param reviewAction A DTO containing the batch ID, the action (APPROVE/REJECT), the reviewer's identifier,
     *                     and a rejection reason (if applicable).
     * @return A {@link ResponseEntity} with a confirmation message.
     */
    @PostMapping("/review/action")
    public ResponseEntity<ApiResponse<String>> processReviewAction(@RequestBody ReviewActionDTO reviewAction) {
        log.info("InventoryHistoryController.processReviewAction >> Entry | Action: '{}' for Batch ID: {}", reviewAction.getAction(), reviewAction.getBatchId());

        inventoryHistoryService.processReviewAction(reviewAction);
        String message = "Inventory batch " + reviewAction.getBatchId() + " has been " + reviewAction.getAction().toString().toLowerCase() + "d.";
        ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, message);

        log.info("InventoryHistoryController.processReviewAction >> Exited Successfully | Action: '{}', Batch ID: {}", reviewAction.getAction(), reviewAction.getBatchId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the final, approved inventory records along with batch details for a specific batch ID.
     * This endpoint should be used after a batch has been successfully approved.
     *
     * @param batchId The unique identifier of the batch.
     * @return A {@link ResponseEntity} containing an {@link ApiResponse} with the {@link ApprovedInventoryBatchDTO}.
     * The DTO includes batch metadata and a list of the final {@link com.tem.be.api.model.FirstNetInventory} records.
     */
    @GetMapping("/batch/{batchId}/approved")
    public ResponseEntity<ApiResponse<ApprovedInventoryBatchDTO>> getApprovedBatch(@PathVariable String batchId) {
        log.info("InventoryHistoryController.getApprovedBatch >> Entry | Batch ID: {}", batchId);

        ApprovedInventoryBatchDTO data = inventoryHistoryService.getApprovedBatchDetails(batchId);
        ApiResponse<ApprovedInventoryBatchDTO> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, data);

        log.info("InventoryHistoryController.getApprovedBatch >> Exited Successfully | Retrieved {} final records for Batch ID: {}", data.getInventoryRecords().size(), batchId);
        return ResponseEntity.ok(response);
    }
}
