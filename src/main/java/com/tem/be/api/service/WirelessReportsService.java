package com.tem.be.api.service;

import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.TotalByCarrierDTO;
import com.tem.be.api.dto.TotalByDepartmentDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierPlansDTO;
import com.tem.be.api.dto.dashboard.DepartmentExpenseDTO;
import com.tem.be.api.dto.dashboard.DepartmentTotalDTO;
import com.tem.be.api.model.WirelessReports;

import java.util.Date;
import java.util.List;

public interface WirelessReportsService {
    List<WirelessReports> getAllWirelessReports();

    DepartmentCarrierDistinctDTO getDistinctDepartmentAndCarrier();

    List<WirelessReports> filterWirelessReports(List<String> departments, List<String> carriers);

    WirelessReports updateById(Long id, ReportUpdateDTO dto);

    List<TotalByDepartmentDTO> getTotalByDepartment();

    List<TotalByCarrierDTO> totalByCarrier();

    List<WirelessReports> getReportsBetweenDates(Date startDate, Date endDate);

    List<DepartmentCarrierPlansDTO> getDepartmentWiseCarrierPlans();

    List<DepartmentTotalDTO> getCarrierTotalsByRange(Date startDate, Date endDate);

    List<WirelessReports> getFilteredReports(Date startDate, Date endDate, String department, String carrier);

    List<DepartmentExpenseDTO> getExpenseSummaryByCarrier(String carrier, Date startDate, Date endDate);
}
