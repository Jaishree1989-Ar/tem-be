package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.dto.inventory.ApprovedInventoryBatchDTO;
import com.tem.be.api.dto.inventory.InventoryBatchReviewDTO;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.service.InventoryHistoryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryHistoryService inventoryHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private InventoryHistory history1;
    private InventoryHistory history2;
    private final String batchId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        history1 = new InventoryHistory();
        history1.setBatchId(batchId);
        history1.setCarrier("FirstNet");

        history2 = new InventoryHistory();
        history2.setBatchId(UUID.randomUUID().toString());
        history2.setCarrier("Verizon");
    }

    /**
     * Tests the GET endpoint for retrieving all inventory histories.
     * Verifies HTTP status 200 OK, content type, and the structure of the returned list.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /inventory-history/get-all - Should return list of histories and status 200 OK")
    void getAllInventoryHistories_shouldReturnListOfHistories() throws Exception {
        // Arrange
        when(inventoryHistoryService.getAllInventoryHistories()).thenReturn(List.of(history1, history2));

        // Act & Assert
        mockMvc.perform(get("/inventory-history/get-all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].batchId", is(batchId)));
    }

    /**
     * Tests the GET endpoint for retrieving a specific inventory batch for review.
     * Verifies HTTP status 200 OK and checks if the returned DTO contains the correct batch ID.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /inventory-history/review/{batchId} - Should return review data and status 200 OK")
    void getBatchForReview_shouldReturnReviewData() throws Exception {
        // Arrange
        InventoryBatchReviewDTO reviewDTO = new InventoryBatchReviewDTO(history1, Collections.emptyList());
        when(inventoryHistoryService.getBatchForReview(batchId)).thenReturn(reviewDTO);

        // Act & Assert
        mockMvc.perform(get("/inventory-history/review/{batchId}", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data.batchDetails.batchId", is(batchId)));
    }

    /**
     * Tests the POST endpoint for processing a review action (e.g., Approve/Reject).
     * Verifies HTTP status 200 OK and the success message in the response body.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("POST /inventory-history/review/action - Should process action and return status 200 OK")
    void processReviewAction_shouldReturnSuccessMessage() throws Exception {
        // Arrange
        ReviewActionDTO reviewAction = new ReviewActionDTO();
        reviewAction.setBatchId(batchId);
        reviewAction.setAction(ReviewActionDTO.ActionType.APPROVE);
        reviewAction.setReviewedBy("testUser");

        doNothing().when(inventoryHistoryService).processReviewAction(any(ReviewActionDTO.class));

        // Act & Assert
        mockMvc.perform(post("/inventory-history/review/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", is("Inventory batch " + batchId + " has been approved.")));
    }

    /**
     * Tests the GET endpoint for retrieving details of an approved inventory batch.
     * Verifies HTTP status 200 OK and checks the structure of the returned approved batch DTO.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /inventory-history/batch/{batchId}/approved - Should return approved data and status 200 OK")
    void getApprovedBatch_shouldReturnApprovedData() throws Exception {
        // Arrange
        ApprovedInventoryBatchDTO approvedDTO = new ApprovedInventoryBatchDTO(history1, Collections.emptyList());
        when(inventoryHistoryService.getApprovedBatchDetails(batchId)).thenReturn(approvedDTO);

        // Act & Assert
        mockMvc.perform(get("/inventory-history/batch/{batchId}/approved", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data.batchDetails.batchId", is(batchId)))
                .andExpect(jsonPath("$.data.inventoryRecords", hasSize(0)));
    }
}