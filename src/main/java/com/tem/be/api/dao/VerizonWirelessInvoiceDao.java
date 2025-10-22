package com.tem.be.api.dao;

import com.tem.be.api.model.VerizonWirelessInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for accessing and managing verizon wireless invoice entities.
 */
@Repository
public interface VerizonWirelessInvoiceDao extends JpaRepository<VerizonWirelessInvoice, Long>, JpaSpecificationExecutor<VerizonWirelessInvoice> {
    List<VerizonWirelessInvoice> findByInvoiceHistory_BatchId(String batchId);

    @Query("SELECT DISTINCT i.department FROM VerizonWirelessInvoice i WHERE i.department IS NOT NULL AND i.department <> '' ORDER BY i.department")
    List<String> findDistinctDepartments();

    @Query("SELECT i.department, i.billContactName, i.accountNumber, i.totalCurrentCharges " +
            "FROM VerizonWirelessInvoice i " +
            "WHERE i.billPeriodStart BETWEEN :startDate AND :endDate " +
            "ORDER BY i.department, i.accountNumber")
    List<Object[]> findExpenseSummaryByDateRange(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}
