package com.tem.be.api.service;

import com.tem.be.api.dao.*;
import com.tem.be.api.dto.WiredReportsFilterDto;
import com.tem.be.api.model.ATTInvoice;
import com.tem.be.api.model.FirstNetInvoice;
import com.tem.be.api.model.VerizonWirelessInvoice;
import com.tem.be.api.model.WiredReports;
import com.tem.be.api.utils.CarrierConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
@Service
@Transactional
@Log4j2
public class WiredReportsServiceImpl implements WiredReportsService {

    private final WiredReportsDao wiredReportsDao;

    @Autowired
    public WiredReportsServiceImpl(WiredReportsDao wiredReportsDao) {
        this.wiredReportsDao = wiredReportsDao;
    }

    /**
     *
     * @param filter   A DTO containing all filter parameters.
     * @return
     */
    @Override
    public List<WiredReports> searchWiredReports(WiredReportsFilterDto filter) {
        Specification<WiredReports> spec = WiredReportsSpecifications.findByCriteria(filter);
        return wiredReportsDao.findAll(spec);
    }

    /**
     *
     * @param id          The ID of the WiredReport to update.
     * @param newViscode  The new viscode value.
     * @param carrier     The carrier associated with the report.
     * @return
     */
    @Override
    @Transactional
    public Object updateViscode(Long id, String newViscode, String carrier) {
        log.info("Attempting to update viscode | ID: {} | Carrier: {} | New Value: {}",
                id, carrier, newViscode);

        return switch (carrier.toLowerCase()) {
            case CarrierConstants.CALNET_LC -> {
                WiredReports report = wiredReportsDao.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("WiredReports not found with ID: " + id));

                report.setViscode(newViscode);

                yield wiredReportsDao.save(report);
            }
            default -> throw new IllegalArgumentException("Unsupported or unknown carrier: " + carrier);
        };
    }
}
