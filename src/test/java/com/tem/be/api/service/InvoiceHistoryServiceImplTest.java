package com.tem.be.api.service;

import com.tem.be.api.dao.InvoiceHistoryDao;
import com.tem.be.api.dto.InvoiceBatchReviewDTO;
import com.tem.be.api.dto.ReviewActionDTO;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InvoiceApprovalException;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.service.strategies.invoice.ApprovalStrategy;
import com.tem.be.api.service.strategies.invoice.ApprovalStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceHistoryServiceImplTest {

    @Mock
    private InvoiceHistoryDao invoiceHistoryDao;
    @Mock
    private ApprovalStrategyFactory approvalStrategyFactory;
    @Mock
    private ApprovalStrategy mockStrategy;

    @Spy
    @InjectMocks
    private InvoiceHistoryServiceImpl invoiceHistoryService;

    private InvoiceHistory history;
    private ReviewActionDTO reviewAction;
    private final String batchId = UUID.randomUUID().toString();
    private final Long historyId = 1L;

    @BeforeEach
    void setUp() {
        history = new InvoiceHistory();
        history.setBatchId(batchId);
        history.setCarrier("FirstNet");
        history.setIsDeleted(false);

        reviewAction = new ReviewActionDTO();
        reviewAction.setBatchId(batchId);
        reviewAction.setReviewedBy("test-user");

        invoiceHistoryService.setSelf(invoiceHistoryService);
    }

    /**
     * Tests the retrieval of all non-deleted invoice histories.
     * Verifies that the correct DAO method is called and returns the expected list.
     */
    @Test
    @DisplayName("getAllInvoiceHistories - Should return non-deleted histories")
    void getAllInvoiceHistories_shouldSucceed() {
        // Arrange
        when(invoiceHistoryDao.findByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(history));

        // Act
        List<InvoiceHistory> result = invoiceHistoryService.getAllInvoiceHistories();

        // Assert
        assertThat(result).hasSize(1);
        verify(invoiceHistoryDao).findByIsDeletedFalseOrderByCreatedAtDesc();
    }

    /**
     * Tests the soft deletion of an invoice history record when the ID is found.
     * Verifies that {@code isDeleted} is set to true and the DAO save method is called.
     */
    @Test
    @DisplayName("deleteInvoiceHistory - Should set isDeleted to true when found")
    void deleteInvoiceHistory_whenFound_shouldReturnTrue() {
        // Arrange
        when(invoiceHistoryDao.findById(historyId)).thenReturn(Optional.of(history));
        ArgumentCaptor<InvoiceHistory> historyCaptor = ArgumentCaptor.forClass(InvoiceHistory.class);

        // Act
        boolean result = invoiceHistoryService.deleteInvoiceHistory(historyId);

        // Assert
        assertTrue(result);
        verify(invoiceHistoryDao).save(historyCaptor.capture());
        assertTrue(historyCaptor.getValue().getIsDeleted());
    }

    /**
     * Tests the soft deletion of an invoice history record when the ID is not found.
     * Verifies that the DAO save method is never called and false is returned.
     */
    @Test
    @DisplayName("deleteInvoiceHistory - Should return false when not found")
    void deleteInvoiceHistory_whenNotFound_shouldReturnFalse() {
        // Arrange
        when(invoiceHistoryDao.findById(historyId)).thenReturn(Optional.empty());

        // Act
        boolean result = invoiceHistoryService.deleteInvoiceHistory(historyId);

        // Assert
        assertFalse(result);
        verify(invoiceHistoryDao, never()).save(any());
    }

    /**
     * Tests the retrieval of batch details for review when the batch is found.
     * Verifies that the correct DAO and Strategy methods are called and a populated DTO is returned.
     */
    @Test
    @DisplayName("getBatchForReview - Should return DTO when batch found")
    void getBatchForReview_whenFound_shouldReturnDTO() {
        // Arrange
        when(invoiceHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));
        when(approvalStrategyFactory.getStrategy(history.getCarrier())).thenReturn(mockStrategy);
        when(mockStrategy.getTemporaryInvoicesForReview(batchId)).thenReturn(Collections.emptyList());

        // Act
        InvoiceBatchReviewDTO result = invoiceHistoryService.getBatchForReview(batchId);

        // Assert
        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("batchDetails", history);

        verify(invoiceHistoryDao).findByBatchId(batchId);
    }

    /**
     * Tests that {@code processReviewAction} delegates to {@code performApproval} for APPROVE action.
     */
    @Test
    @DisplayName("processReviewAction - Should call performApproval for APPROVE action")
    void processReviewAction_withApprove_shouldCallPerformApproval() {
        // Arrange
        reviewAction.setAction(ReviewActionDTO.ActionType.APPROVE);
        doNothing().when(invoiceHistoryService).performApproval(reviewAction);

        // Act
        invoiceHistoryService.processReviewAction(reviewAction);

        // Assert
        verify(invoiceHistoryService).performApproval(reviewAction);
        verify(invoiceHistoryService, never()).performRejection(any());
    }

    /**
     * Tests that {@code processReviewAction} delegates to {@code performRejection} for REJECT action.
     */
    @Test
    @DisplayName("processReviewAction - Should call performRejection for REJECT action")
    void processReviewAction_withReject_shouldCallPerformRejection() {
        // Arrange
        reviewAction.setAction(ReviewActionDTO.ActionType.REJECT);
        doNothing().when(invoiceHistoryService).performRejection(reviewAction);

        // Act
        invoiceHistoryService.processReviewAction(reviewAction);

        // Assert
        verify(invoiceHistoryService).performRejection(reviewAction);
        verify(invoiceHistoryService, never()).performApproval(any());
    }

    /**
     * Tests exception handling within {@code processReviewAction}.
     * Verifies that an exception during approval triggers {@code finalizeAsFailed}
     * and re-throws an {@link InvoiceApprovalException}.
     */
    @Test
    @DisplayName("processReviewAction - Should finalize as FAILED when approval throws exception")
    void processReviewAction_whenApprovalFails_shouldFinalizeAsFailed() {
        // Arrange
        reviewAction.setAction(ReviewActionDTO.ActionType.APPROVE);
        doThrow(new RuntimeException("Database error")).when(invoiceHistoryService).performApproval(reviewAction);
        doNothing().when(invoiceHistoryService).finalizeAsFailed(any(), anyString());

        // Act & Assert
        assertThrows(InvoiceApprovalException.class, () ->
                invoiceHistoryService.processReviewAction(reviewAction)
        );

        verify(invoiceHistoryService).finalizeAsFailed(eq(reviewAction), anyString());
    }

    /**
     * Tests the successful execution of the approval workflow.
     * Verifies that the history status is updated to APPROVED, the reviewer is set,
     * and the strategy's approve method is called.
     */
    @Test
    @DisplayName("performApproval - Should update status to APPROVED")
    void performApproval_shouldSucceed() {
        // Arrange
        when(invoiceHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));
        when(approvalStrategyFactory.getStrategy(history.getCarrier())).thenReturn(mockStrategy);

        // Act
        invoiceHistoryService.performApproval(reviewAction);

        // Assert
        verify(mockStrategy).approve(history);
        verify(invoiceHistoryDao).save(history);
        assertThat(history.getStatus()).isEqualTo(InvoiceStatus.APPROVED);
        assertThat(history.getReviewedBy()).isEqualTo("test-user");
        assertThat(history.getRejectionReason()).isNull();
    }

    /**
     * Tests that retrieving final approved batch details throws an exception if the batch status is not APPROVED.
     * Verifies that an {@link IllegalStateException} is thrown.
     */
    @Test
    @DisplayName("getApprovedBatchDetails - Should throw IllegalStateException if not approved")
    void getApprovedBatchDetails_whenNotApproved_shouldThrowException() {
        // Arrange
        history.setStatus(InvoiceStatus.REJECTED);
        when(invoiceHistoryDao.findByBatchId(batchId)).thenReturn(Optional.of(history));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                invoiceHistoryService.getApprovedBatchDetails(batchId)
        );
        assertThat(ex.getMessage()).contains("Batch is not approved");
    }
}