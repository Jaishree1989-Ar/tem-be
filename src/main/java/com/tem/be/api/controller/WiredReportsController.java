package com.tem.be.api.controller;

import com.tem.be.api.dto.UpdateViscodeRequest;
import com.tem.be.api.dto.WiredReportsFilterDto;
import com.tem.be.api.model.WiredReports;
import com.tem.be.api.service.WiredReportsService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


/**
 * REST controller for managing Wired reports related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/wiredReports")
public class WiredReportsController {

    private final WiredReportsService wiredReportsService;

    /**
     * @param wiredReportsService the service handling Wired reports operations
     */
    @Autowired
    public WiredReportsController(WiredReportsService wiredReportsService) {
        this.wiredReportsService = wiredReportsService;
    }

    /**
     * Searches for Wired reports with dynamic filter criteria.
     *
     * @param filter A DTO containing all filter parameters (e.g., carrier, startDate, endDate).
     * @return A list of wired reports matching the criteria.
     */
    @GetMapping(value = "/searchWiredReports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<WiredReports>>> searchWiredReports(
            WiredReportsFilterDto filter) { // Only the DTO is needed now

        log.info("WiredReportsController.searchWiredReports() >> Entered with filter: {}", filter);

        List<WiredReports> results = wiredReportsService.searchWiredReports(filter);

        ApiResponse<List<WiredReports>> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", results);

        log.info("WiredReportsController.searchWiredReports() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a wired report to set a new value for viscode.
     *
     * @param id      The ID of the report to be updated.
     * @param request The request body containing the new viscode and carrier.
     * @return A ResponseEntity containing the updated report.
     */
    @PatchMapping("/{id}/viscode")
    public ResponseEntity<ApiResponse<Object>> updateViscode(
            @PathVariable Long id,
            @Valid @RequestBody UpdateViscodeRequest request) {

        log.info("WiredReportsController.updateViscode >> Entry | ID: {} | Carrier: {}", id, request.getCarrier());

        Object updatedReport = wiredReportsService.updateViscode(
                id,
                request.getNewViscode(),
                request.getCarrier()
        );

        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Successfully updated viscode.",
                updatedReport
        );
        log.info("WiredReportsController.updateViscode >> Exit");
        return ResponseEntity.ok(response);
    }
}
