package com.tem.be.api.controller;

import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.model.TelecomReports;
import com.tem.be.api.service.TelecomReportsService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing telecom reports related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/telecomReports")
public class TelecomReportsController {

    private final TelecomReportsService telecomReportsService;

    /**
     * @param telecomReportsService the service handling telecom reports operations
     */
    @Autowired
    public TelecomReportsController(TelecomReportsService telecomReportsService) {
        this.telecomReportsService = telecomReportsService;
    }

    /**
     * Returns all telecom reports.
     */
    @GetMapping(value = "/getAllTelecomReports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<TelecomReports>>> getAllTelecomReports() {
        log.info("TelecomReportsController.getAllTelecomReports() >> Entered");

        List<TelecomReports> tReports = telecomReportsService.getAllTelecomReports();
        ApiResponse<List<TelecomReports>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING,tReports);

        log.info("TelecomReportsController.getAllTelecomReports() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Updates a telecom report by ID.
     *
     * @param id report ID
     * @param dto updated report details
     */
    @PutMapping(value = "/updateById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TelecomReports> updateById(@PathVariable("id") Long id, @RequestBody ReportUpdateDTO dto) {
        log.info("TelecomReportsController.updateById() >> Entered");

        TelecomReports updateTelecom = telecomReportsService.updateById(id, dto);

        log.info("TelecomReportsController.updateById() >> Exited");
        return new ResponseEntity<>(updateTelecom, HttpStatus.OK);
    }

    /**
     * Returns distinct department and carrier combinations from telecom reports.
     */
    @GetMapping(value = "/getDistinctDepartmentsAndCarriers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DepartmentCarrierDistinctDTO>> getDistinctDepartmentsAndCarriers() {
        log.info("TelecomReportsController.getDistinctDepartmentsAndCarriers() >> Entered");

        DepartmentCarrierDistinctDTO result = telecomReportsService.getDistinctDepartmentAndCarrier();
        ApiResponse<DepartmentCarrierDistinctDTO> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, result);

        log.info("TelecomReportsController.getDistinctDepartmentsAndCarriers() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Filters telecom reports based on departments and carriers.
     *
     * @param departments optional list of departments
     * @param carriers optional list of carriers
     */
    @GetMapping(value = "/filterTelecomReports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<TelecomReports>>> filterTelecomReports(
            @RequestParam(required = false) List<String> departments,
            @RequestParam(required = false) List<String> carriers) {

        log.info("TelecomReportsController.filterTelecomReports() >> Entered");
        List<TelecomReports> filteredReports = telecomReportsService.telecomReportsService(departments, carriers);
        ApiResponse<List<TelecomReports>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, filteredReports);

        log.info("TelecomReportsController.filterTelecomReports() >> Exited");
        return ResponseEntity.ok(response);
    }

}
