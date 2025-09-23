package com.tem.be.api.service;

import com.tem.be.api.dao.TelecomReportsDao;
import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.TelecomReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for telecom reports related operations.
 */
@Service
@Transactional
public class TelecomReportsServiceImpl implements TelecomReportsService {


    private final TelecomReportsDao telecomReportsDao;

    @Autowired
    public TelecomReportsServiceImpl(TelecomReportsDao telecomReportsDao) {
        this.telecomReportsDao = telecomReportsDao;
    }

    @Override
    public List<TelecomReports> getAllTelecomReports() {
        return telecomReportsDao.findAll();
    }

    @Override
    public TelecomReports updateById(Long id, ReportUpdateDTO dto) {
        TelecomReports existingTelecom = telecomReportsDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Telecom report not found with ID: " + id));
        existingTelecom.setDepartment(dto.getDepartment());
        existingTelecom.setViscode(dto.getViscode());
        existingTelecom.setNameOnInvoice(dto.getNameOnInvoice());
        return telecomReportsDao.save(existingTelecom);
    }

    @Override
    public DepartmentCarrierDistinctDTO getDistinctDepartmentAndCarrier() {
        List<String> departments = telecomReportsDao.findDistinctDepartments();
        List<String> carriers = telecomReportsDao.findDistinctCarriers();
        List<String> deviceClasses = telecomReportsDao.findDistinctDeviceClasses();

        return new DepartmentCarrierDistinctDTO(departments, carriers, deviceClasses);
    }

    @Override
    public List<TelecomReports> telecomReportsService(List<String> departments, List<String> carriers) {
        boolean hasDepartments = departments != null && !departments.isEmpty();
        boolean hasCarriers = carriers != null && !carriers.isEmpty();

        if (hasDepartments && hasCarriers) {
            return telecomReportsDao.findByDepartmentsAndCarriers(departments, carriers);
        } else if (hasDepartments) {
            return telecomReportsDao.findByDepartments(departments);
        } else if (hasCarriers) {
            return telecomReportsDao.findByCarriers(carriers);
        } else {
            return telecomReportsDao.findAll(); // No filters
        }
    }
}
