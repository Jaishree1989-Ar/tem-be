package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.ApprovedInvoiceBatchDTO;
import com.tem.be.api.dto.InvoiceBatchReviewDTO;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.service.InvoiceHistoryService;
import com.tem.be.api.utils.RestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceHistoryService invoiceHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private InvoiceHistory history1;
    private final String batchId = UUID.randomUUID().toString();
    private final Long historyId = 1L;

    @BeforeEach
    void setUp() {
        history1 = new InvoiceHistory();
        history1.setBatchId(batchId);
        history1.setCarrier("FirstNet");
    }

    /**
     * Tests the GET endpoint for retrieving all invoice histories.
     * Verifies HTTP status 200 OK, content type, and the structure of the returned list.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /invoice-history/get-all - Should return list of histories and status 200 OK")
    void getAllInvoiceHistories_shouldReturnListOfHistories() throws Exception {
        when(invoiceHistoryService.getAllInvoiceHistories()).thenReturn(List.of(history1));

        mockMvc.perform(get("/invoice-history/get-all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(RestConstants.SUCCESS_CODE)))
                .andExpect(jsonPath("$.status", is(RestConstants.SUCCESS_STRING)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].batchId", is(batchId)));
    }

    /**
     * Tests the DELETE endpoint for soft deleting an invoice history record with a valid ID.
     * Verifies HTTP status 200 OK and a success message.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("DELETE /invoice-history/deleteById/{id} - Should return success on valid ID")
    void softDeleteInvoiceHistory_whenFound_shouldReturnSuccess() throws Exception {
        when(invoiceHistoryService.deleteInvoiceHistory(historyId)).thenReturn(true);

        mockMvc.perform(delete("/invoice-history/deleteById/{id}", historyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(RestConstants.SUCCESS_CODE)))
                .andExpect(jsonPath("$.data", is("Invoice history soft deleted successfully.")));
    }

    /**
     * Tests the DELETE endpoint when the invoice history ID is not found.
     * Verifies HTTP status 404 Not Found and the appropriate error message.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("DELETE /invoice-history/deleteById/{id} - Should return 404 on invalid ID")
    void softDeleteInvoiceHistory_whenNotFound_shouldReturnNotFound() throws Exception {
        when(invoiceHistoryService.deleteInvoiceHistory(historyId)).thenReturn(false);

        mockMvc.perform(delete("/invoice-history/deleteById/{id}", historyId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode", is(RestConstants.NO_DATA_CODE)))
                .andExpect(jsonPath("$.data", is("Invoice history record not found.")));
    }

    /**
     * Tests the GET endpoint for retrieving a specific invoice batch for review.
     * Verifies HTTP status 200 OK and checks if the returned DTO contains the correct batch ID.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /invoice-history/review/{batchId} - Should return review data")
    void getBatchForReview_shouldReturnReviewData() throws Exception {
        // Arrange
        InvoiceBatchReviewDTO reviewDTO = new InvoiceBatchReviewDTO(history1, Collections.emptyList());
        when(invoiceHistoryService.getBatchForReview(batchId)).thenReturn(reviewDTO);

        // Act & Assert
        mockMvc.perform(get("/invoice-history/review/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.batchDetails.batchId", is(batchId)));
    }

    /**
     * Tests the POST endpoint for processing a review action (e.g., Approve/Reject).
     * Verifies HTTP status 200 OK and the success message in the response body.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("POST /invoice-history/review/action - Should process action and return confirmation")
    void processReviewAction_shouldReturnSuccessMessage() throws Exception {
        // Arrange
        ReviewActionDTO actionDTO = new ReviewActionDTO();
        actionDTO.setBatchId(batchId);
        actionDTO.setAction(ReviewActionDTO.ActionType.APPROVE);
        doNothing().when(invoiceHistoryService).processReviewAction(any(ReviewActionDTO.class));

        // Act & Assert
        mockMvc.perform(post("/invoice-history/review/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actionDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", is("Batch " + batchId + " has been approved.")));
    }

    /**
     * Tests the GET endpoint for retrieving details of an approved invoice batch.
     * Verifies HTTP status 200 OK and checks the structure of the returned approved batch DTO.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /invoice-history/batch/{batchId}/approved - Should return approved data")
    void getApprovedBatchWithInvoices_shouldReturnApprovedData() throws Exception {
        // Arrange
        ApprovedInvoiceBatchDTO approvedDTO = new ApprovedInvoiceBatchDTO(history1, Collections.emptyList());
        when(invoiceHistoryService.getApprovedBatchDetails(batchId)).thenReturn(approvedDTO);

        // Act & Assert
        mockMvc.perform(get("/invoice-history/batch/{batchId}/approved", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(RestConstants.SUCCESS_CODE)))
                .andExpect(jsonPath("$.data.batchDetails.batchId", is(batchId)));
    }
}