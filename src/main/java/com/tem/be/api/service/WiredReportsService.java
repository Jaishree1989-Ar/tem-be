package com.tem.be.api.service;

import com.tem.be.api.dto.WiredReportsFilterDto;
import com.tem.be.api.model.WiredReports;

import java.util.List;

public interface WiredReportsService {

    /**
     * Searches for WiredReports with dynamic filter criteria.
     *
     * @param filter   A DTO containing all filter parameters.
     * @return A list of reports matching the criteria.
     */
    List<WiredReports> searchWiredReports(WiredReportsFilterDto filter);

    /**
     * Updates the viscode for a given report ID and carrier.
     *
     * @param id          The ID of the WiredReport to update.
     * @param newViscode  The new viscode value.
     * @param carrier     The carrier associated with the report.
     * @return The updated WiredReports object.
     */
    Object updateViscode(Long id, String newViscode, String carrier);
}
