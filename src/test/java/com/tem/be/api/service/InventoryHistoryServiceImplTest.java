package com.tem.be.api.service;

import com.tem.be.api.dao.inventory.InventoryHistoryDao;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.dto.inventory.InventoryBatchReviewDTO;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InventoryApprovalException;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.service.strategies.inventory.InventoryApprovalStrategy;
import com.tem.be.api.service.strategies.inventory.InventoryApprovalStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryHistoryServiceImplTest {

    @Mock
    private InventoryHistoryDao inventoryHistoryDao;
    @Mock
    private InventoryApprovalStrategyFactory strategyFactory;
    @Mock
    private InventoryApprovalStrategy mockStrategy;

    @Spy
    @InjectMocks
    private InventoryHistoryServiceImpl inventoryHistoryService;

    private InventoryHistory history;
    private ReviewActionDTO reviewAction;
    private final String batchId = UUID.randomUUID().toString();
    private final String carrier = "FirstNet";

    @BeforeEach
    void setUp() {
        history = new InventoryHistory();
        history.setInventoryHistoryId(1L);
        history.setBatchId(batchId);
        history.setCarrier(carrier);

        reviewAction = new ReviewActionDTO();
        reviewAction.setBatchId(batchId);
        reviewAction.setReviewedBy("testUser");

        inventoryHistoryService.setSelf(inventoryHistoryService);
    }

    /**
     * Tests the creation of an inventory history record.
     * Verifies that the DAO's save method is called and the saved object is returned.
     */
    @Test
    @DisplayName("createInventoryHistory - Should save and return the history object")
    void createInventoryHistory_shouldSaveAndReturnHistory() {
        // Arrange
        InventoryHistory newHistory = new InventoryHistory();
        newHistory.setBatchId(batchId);
        newHistory.setCarrier(carrier);

        when(inventoryHistoryDao.save(any(InventoryHistory.class))).thenReturn(newHistory);

        // Act
        InventoryHistory savedHistory = inventoryHistoryService.createInventoryHistory(newHistory);

        // Assert
        verify(inventoryHistoryDao).save(newHistory);
        assertThat(savedHistory).isNotNull();
        assertThat(savedHistory.getBatchId()).isEqualTo(batchId);
    }

    /**
     * Tests retrieval of batch details for review when the batch is found.
     * Verifies that the correct DAO and Strategy methods are called and a populated DTO is returned.
     */
    @Test
    @DisplayName("getBatchForReview - Should return DTO when batch is found")
    void getBatchForReview_whenFound_shouldReturnDTO() {
        // Arrange
        when(inventoryHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));
        when(strategyFactory.getStrategy(carrier)).thenReturn(mockStrategy);
        when(mockStrategy.getTemporaryInventoriesForReview(batchId)).thenReturn(Collections.emptyList());

        // Act
        InventoryBatchReviewDTO result = inventoryHistoryService.getBatchForReview(batchId);

        // Assert
        assertThat(result)
                .as("The result DTO should not be null and must contain the correct history object.")
                .isNotNull()
                .hasFieldOrPropertyWithValue("batchDetails", history);

        verify(inventoryHistoryDao).findByBatchId(batchId);
    }

    /**
     * Tests retrieval of batch details when the batch ID does not exist.
     * Verifies that an {@link EntityNotFoundException} is thrown.
     */
    @Test
    @DisplayName("getBatchForReview - Should throw EntityNotFoundException when batch is not found")
    void getBatchForReview_whenNotFound_shouldThrowException() {
        // Arrange
        when(inventoryHistoryDao.findByBatchId(batchId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                inventoryHistoryService.getBatchForReview(batchId)
        );
        assertEquals("Inventory batch not found with ID: " + batchId, exception.getMessage());
    }

    /**
     * Tests that {@code processReviewAction} delegates to {@code performApproval} for APPROVE action.
     */
    @Test
    @DisplayName("processReviewAction - Should call performApproval for APPROVE action")
    void processReviewAction_withApprove_shouldCallPerformApproval() {
        // Arrange
        reviewAction.setAction(ReviewActionDTO.ActionType.APPROVE);
        doNothing().when(inventoryHistoryService).performApproval(reviewAction);

        // Act
        inventoryHistoryService.processReviewAction(reviewAction);

        // Assert
        verify(inventoryHistoryService).performApproval(reviewAction);
        verify(inventoryHistoryService, never()).performRejection(any());
    }

    /**
     * Tests that {@code processReviewAction} delegates to {@code performRejection} for REJECT action.
     */
    @Test
    @DisplayName("processReviewAction - Should call performRejection for REJECT action")
    void processReviewAction_withReject_shouldCallPerformRejection() {
        // Arrange
        reviewAction.setAction(ReviewActionDTO.ActionType.REJECT);
        doNothing().when(inventoryHistoryService).performRejection(reviewAction);

        // Act
        inventoryHistoryService.processReviewAction(reviewAction);

        // Assert
        verify(inventoryHistoryService).performRejection(reviewAction);
        verify(inventoryHistoryService, never()).performApproval(any());
    }

    /**
     * Tests exception handling within {@code processReviewAction}.
     * Verifies that an exception during approval triggers {@code asynchronouslyFinalizeAsFailed}
     * and re-throws an {@link InventoryApprovalException}.
     */
    @Test
    @DisplayName("processReviewAction - Should handle exceptions during approval and finalize as failed")
    void processReviewAction_whenApprovalFails_shouldFinalizeAsFailed() {
        // Arrange
        reviewAction.setAction(ReviewActionDTO.ActionType.APPROVE);
        doThrow(new RuntimeException("DB error")).when(inventoryHistoryService).performApproval(reviewAction);
        doNothing().when(inventoryHistoryService).asynchronouslyFinalizeAsFailed(any(), anyString());

        // Act & Assert
        assertThrows(InventoryApprovalException.class, () ->
                inventoryHistoryService.processReviewAction(reviewAction)
        );

        verify(inventoryHistoryService).asynchronouslyFinalizeAsFailed(eq(reviewAction), anyString());
    }

    /**
     * Tests the successful execution of the approval workflow.
     * Verifies that the history status is updated to APPROVED and the strategy's approve method is called.
     */
    @Test
    @DisplayName("performApproval - Should update history status to APPROVED and save")
    void performApproval_shouldSucceed() {
        // Arrange
        when(inventoryHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));
        when(strategyFactory.getStrategy(carrier)).thenReturn(mockStrategy);
        doNothing().when(mockStrategy).approve(history);
        when(inventoryHistoryDao.save(any(InventoryHistory.class))).thenReturn(history);

        // Act
        inventoryHistoryService.performApproval(reviewAction);

        // Assert: Chained assertions for the history object
        assertThat(history)
                .isNotNull()
                .satisfies(h -> {
                    assertThat(h.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
                    assertThat(h.getReviewedBy()).isEqualTo("testUser");
                    assertThat(h.getReviewedAt()).isNotNull();
                });
        verify(inventoryHistoryDao).save(history);
    }

    /**
     * Tests the successful execution of the rejection workflow.
     * Verifies that the history status is updated to REJECTED, rejection reason is set,
     * and the strategy's reject method is called.
     */
    @Test
    @DisplayName("performRejection - Should update history status to REJECTED and save")
    void performRejection_shouldSucceed() {
        // Arrange
        reviewAction.setRejectionReason("Test rejection");
        when(inventoryHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));
        when(strategyFactory.getStrategy(carrier)).thenReturn(mockStrategy);
        doNothing().when(mockStrategy).reject(batchId);
        when(inventoryHistoryDao.save(any(InventoryHistory.class))).thenReturn(history);

        // Act
        inventoryHistoryService.performRejection(reviewAction);

        // Assert: Chained assertions for the history object
        assertThat(history)
                .isNotNull()
                .satisfies(h -> {
                    assertThat(h.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
                    assertThat(h.getRejectionReason()).isEqualTo("Test rejection");
                    assertThat(h.getReviewedBy()).isEqualTo("testUser");
                });
        verify(inventoryHistoryDao).save(history);
    }

    /**
     * Tests that retrieving final approved batch details throws an exception if the batch status is not APPROVED.
     * Verifies that an {@link IllegalStateException} is thrown.
     */
    @Test
    @DisplayName("getApprovedBatchDetails - Should throw IllegalStateException if batch not approved")
    void getApprovedBatchDetails_whenNotApproved_shouldThrowException() {
        // Arrange
        history.setStatus(InvoiceStatus.REJECTED);
        when(inventoryHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                inventoryHistoryService.getApprovedBatchDetails(batchId)
        );
        assertTrue(exception.getMessage().contains("Cannot retrieve final records for a batch that is not approved"));
    }
}