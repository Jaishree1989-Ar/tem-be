package com.tem.be.api.controller;

import com.tem.be.api.dto.ApprovedInvoiceBatchDTO;
import com.tem.be.api.dto.InvoiceBatchReviewDTO;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.model.FirstNetInvoice;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.service.InvoiceHistoryService;
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
@RequestMapping("/invoice-history")
public class InvoiceHistoryController {

    private final InvoiceHistoryService invoiceHistoryService;

    @Autowired
    public InvoiceHistoryController(InvoiceHistoryService invoiceHistoryService) {
        this.invoiceHistoryService = invoiceHistoryService;
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<InvoiceHistory>>> getAllInvoiceHistories() {
        List<InvoiceHistory> histories = invoiceHistoryService.getAllInvoiceHistories();

        ApiResponse<List<InvoiceHistory>> response = new ApiResponse<>(
                RestConstants.SUCCESS_CODE,
                RestConstants.SUCCESS_STRING,
                histories
        );
        log.info("InvoiceHistoryController.getAllInvoiceHistories() >> Retrieved Successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<ApiResponse<String>> softDeleteInvoiceHistory(@PathVariable Long id) {
        boolean deleted = invoiceHistoryService.deleteInvoiceHistory(id);

        if (deleted) {
            ApiResponse<String> response = new ApiResponse<>(
                    RestConstants.SUCCESS_CODE,
                    RestConstants.SUCCESS_STRING,
                    "Invoice history soft deleted successfully."
            );
            log.info("InvoiceHistoryController.softDeleteInvoiceHistory() >> Deleted Successfully");
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<String> response = new ApiResponse<>(
                    RestConstants.NO_DATA_CODE,
                    RestConstants.FAIL_STRING,
                    "Invoice history record not found."
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Endpoint to fetch the details of an uploaded batch for review.
     *
     * @param batchId The unique identifier for the batch to be reviewed.
     * @return A ResponseEntity containing an ApiResponse with the batch data in an {@link InvoiceBatchReviewDTO}.
     */
    @GetMapping("/review/{batchId}")
    public ResponseEntity<ApiResponse<InvoiceBatchReviewDTO>> getBatchForReview(@PathVariable String batchId) {
        log.info("InvoiceHistoryController.getBatchForReview() >> Entry | Batch ID: {}", batchId);
        InvoiceBatchReviewDTO reviewData = invoiceHistoryService.getBatchForReview(batchId);
        ApiResponse<InvoiceBatchReviewDTO> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", reviewData);
        log.info("InvoiceHistoryController.getBatchForReview() >> Exited Successfully | Batch ID: {}", batchId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to approve or reject a batch of invoice data.
     *
     * @param reviewAction A DTO containing the batch ID and the action to perform (APPROVE or REJECT).
     * @return A ResponseEntity with a confirmation message.
     */
    @PostMapping("/review/action")
    public ResponseEntity<ApiResponse<String>> processReviewAction(@RequestBody ReviewActionDTO reviewAction) {
        log.info("InvoiceHistoryController.processReviewAction() >> Entry | Action: '{}' for Batch ID: {}", reviewAction.getAction(), reviewAction.getBatchId());
        invoiceHistoryService.processReviewAction(reviewAction);
        String message = "Batch " + reviewAction.getBatchId() + " has been " + reviewAction.getAction().toString().toLowerCase() + "d.";
        ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", message);
        log.info("InvoiceHistoryController.processReviewAction() >> Exited Successfully | Action: '{}', Batch ID: {}", reviewAction.getAction(), reviewAction.getBatchId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the final, approved invoices along with batch details for a specific batch ID.
     * This endpoint should be used after a batch has been approved.
     *
     * @param batchId The unique identifier of the batch.
     * @return A ResponseEntity containing an ApiResponse with the batch details and a list of {@link FirstNetInvoice} records.
     */
    @GetMapping("/batch/{batchId}/approved")
    public ResponseEntity<ApiResponse<ApprovedInvoiceBatchDTO>> getApprovedBatchWithInvoices(@PathVariable String batchId) {
        log.info("Controller request to get approved batch details for batch ID: {}", batchId);

        ApprovedInvoiceBatchDTO data = invoiceHistoryService.getApprovedBatchDetails(batchId);

        ApiResponse<ApprovedInvoiceBatchDTO> response = new ApiResponse<>(
                RestConstants.SUCCESS_CODE,
                RestConstants.SUCCESS_STRING,
                data
        );
        log.info("Successfully retrieved batch details and {} invoices for batch ID: {}", data.getInvoiceRecords().size(), batchId);
        return ResponseEntity.ok(response);
    }
}