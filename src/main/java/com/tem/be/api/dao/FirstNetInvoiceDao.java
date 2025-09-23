package com.tem.be.api.dao;

import com.tem.be.api.model.FirstNetInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for accessing and managing firstnet invoice entities.
 */
@Repository
public interface FirstNetInvoiceDao extends JpaRepository<FirstNetInvoice, Long>, JpaSpecificationExecutor<FirstNetInvoice> {

    @Query("SELECT DISTINCT f.department FROM FirstNetInvoice f WHERE f.department IS NOT NULL")
    List<String> findDistinctDepartments();

    /**
     * Finds all FirstNetInvoice records associated with a specific InvoiceHistory batch ID.
     * Spring Data JPA will generate the query to join InvoiceHistory and filter by batchId.
     *
     * @param batchId The unique batch identifier from the InvoiceHistory record.
     * @return A list of final invoices for that batch.
     */
    List<FirstNetInvoice> findByInvoiceHistory_BatchId(String batchId);

    @Query("SELECT f.department, f.userName, f.wirelessNumber, f.totalCurrentCharges FROM FirstNetInvoice f WHERE f.invoiceDate BETWEEN :startDate AND :endDate ORDER BY f.department, f.wirelessNumber")
    List<Object[]> findExpenseSummaryByDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}
