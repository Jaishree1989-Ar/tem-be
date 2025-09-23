package com.tem.be.api.dao;

import com.tem.be.api.model.ATTInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for accessing and managing AT & T Mobility invoice entities.
 */
@Repository
public interface ATTInvoiceDao extends JpaRepository<ATTInvoice, Long>, JpaSpecificationExecutor<ATTInvoice> {
    List<ATTInvoice> findByInvoiceHistory_BatchId(String batchId);

    @Query("SELECT DISTINCT a.department FROM ATTInvoice a WHERE a.department IS NOT NULL ORDER BY a.department")
    List<String> findDistinctDepartments();

    @Query("SELECT f.department, f.userName, f.wirelessNumber, f.totalCurrentCharges FROM ATTInvoice f WHERE f.invoiceDate BETWEEN :startDate AND :endDate ORDER BY f.department, f.wirelessNumber")
    List<Object[]> findExpenseSummaryByDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}
