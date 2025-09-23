package com.tem.be.api.dao;

import com.tem.be.api.model.WirelessReports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for accessing WirelessReports data.
 * Provides custom queries for filtering and aggregating report data.
 */
@Repository
public interface WirelessReportsDao extends JpaRepository<WirelessReports, Long> {

    @Query("SELECT DISTINCT t.department FROM WirelessReports t WHERE t.department IS NOT NULL")
    List<String> findDistinctDepartments();

    @Query("SELECT DISTINCT t.carrier FROM WirelessReports t WHERE t.carrier IS NOT NULL")
    List<String> findDistinctCarriers();

    @Query("SELECT DISTINCT t.deviceClass FROM WirelessReports t WHERE t.deviceClass IS NOT NULL")
    List<String> findDistinctDeviceClasses();

    @Query("SELECT t FROM WirelessReports t WHERE t.department IN :departments AND t.carrier IN :carriers")
    List<WirelessReports> findByDepartmentsAndCarriers(
            @Param("departments") List<String> departments,
            @Param("carriers") List<String> carriers);

    @Query("SELECT t FROM WirelessReports t WHERE t.department IN :departments")
    List<WirelessReports> findByDepartments(
            @Param("departments") List<String> departments);

    @Query("SELECT t FROM WirelessReports t WHERE t.carrier IN :carriers")
    List<WirelessReports> findByCarriers(
            @Param("carriers") List<String> carriers);

    @Query("SELECT w.department AS department, SUM(w.total) AS total FROM WirelessReports w GROUP BY w.department")
    List<Object[]> getTotalByDepartment();

    @Query("SELECT w.carrier AS carrier, SUM(w.total) AS total FROM WirelessReports w GROUP BY w.carrier")
    List<Object[]> totalByCarrier();

    List<WirelessReports> findByLastInvoiceOnBetween(Date startDate, Date endDate);

    @Query(value = """
                SELECT department, carrier, GROUP_CONCAT(DISTINCT service_plan) AS plans
                FROM wireless_reports
                WHERE service_plan IS NOT NULL AND service_plan != ''
                AND department IS NOT NULL AND department != ''
                GROUP BY department, carrier
            """, nativeQuery = true)
    List<Object[]> getGroupedPlansByDepartmentAndCarrier();

    @Query(value = """
                SELECT department, carrier, account, SUM(total) as total
                FROM wireless_reports
                WHERE last_invoice_on BETWEEN :startDate AND :endDate
                GROUP BY department, carrier, account
            """, nativeQuery = true)
    List<Object[]> getCarrierTotalsBetweenDates(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT r FROM WirelessReports r WHERE " +
            "(:startDate IS NULL OR r.lastInvoiceOn >= :startDate) AND " +
            "(:endDate IS NULL OR r.lastInvoiceOn <= :endDate) AND " +
            "(:department IS NULL OR r.department = :department) AND " +
            "(:carrier IS NULL OR r.carrier = :carrier)")
    List<WirelessReports> findWithOptionalFilters(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("department") String department,
            @Param("carrier") String carrier
    );
}
