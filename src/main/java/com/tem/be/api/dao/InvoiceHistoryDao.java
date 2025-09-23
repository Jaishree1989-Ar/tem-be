package com.tem.be.api.dao;

import com.tem.be.api.model.InvoiceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceHistoryDao extends JpaRepository<InvoiceHistory, Long> {

    List<InvoiceHistory> findByIsDeletedFalseOrderByCreatedAtDesc();

    /**
     * Finds an InvoiceHistory entity by its public batchId and eagerly fetches
     * the associated temporary invoices in a single database query to prevent
     * the N+1 problem.
     *
     * @param batchId The public UUID for the batch.
     * @return An Optional containing the InvoiceHistory with its temp invoices populated.
     */
    @Query("SELECT h FROM InvoiceHistory h LEFT JOIN FETCH h.tempInvoices WHERE h.batchId = :batchId")
    Optional<InvoiceHistory> findByBatchIdWithInvoices(String batchId);

    Optional<InvoiceHistory> findByBatchId(String batchId);
}
