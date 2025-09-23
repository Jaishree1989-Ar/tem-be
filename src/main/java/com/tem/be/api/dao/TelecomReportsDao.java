package com.tem.be.api.dao;

import com.tem.be.api.model.TelecomReports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for accessing TelecomReports data.
 * Provides custom queries for filtering and aggregating report data.
 */
@Repository
public interface TelecomReportsDao extends JpaRepository<TelecomReports, Long> {

    @Query("SELECT DISTINCT t.department FROM TelecomReports t WHERE t.department IS NOT NULL")
    List<String> findDistinctDepartments();

    @Query("SELECT DISTINCT t.carrier FROM TelecomReports t WHERE t.carrier IS NOT NULL")
    List<String> findDistinctCarriers();

    @Query("SELECT DISTINCT t.deviceClass FROM TelecomReports t WHERE t.deviceClass IS NOT NULL")
    List<String> findDistinctDeviceClasses();

    @Query("SELECT t FROM TelecomReports t WHERE t.department IN :departments AND t.carrier IN :carriers")
    List<TelecomReports> findByDepartmentsAndCarriers(
            @Param("departments") List<String> departments,
            @Param("carriers") List<String> carriers);

    @Query("SELECT t FROM TelecomReports t WHERE t.department IN :departments")
    List<TelecomReports> findByDepartments(
            @Param("departments") List<String> departments);

    @Query("SELECT t FROM TelecomReports t WHERE t.carrier IN :carriers")
    List<TelecomReports> findByCarriers(
            @Param("carriers") List<String> carriers);
}

