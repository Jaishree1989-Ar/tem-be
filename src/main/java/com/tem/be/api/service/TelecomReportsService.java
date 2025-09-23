package com.tem.be.api.service;

import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.model.TelecomReports;

import java.util.List;

public interface TelecomReportsService {
    List<TelecomReports> getAllTelecomReports();

    TelecomReports updateById(Long id, ReportUpdateDTO dto);

    DepartmentCarrierDistinctDTO getDistinctDepartmentAndCarrier();

    List<TelecomReports> telecomReportsService(List<String> departments, List<String> carriers);
}
