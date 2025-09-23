package com.tem.be.api.controller;

import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.TotalByCarrierDTO;
import com.tem.be.api.dto.TotalByDepartmentDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierPlansDTO;
import com.tem.be.api.dto.dashboard.DepartmentExpenseDTO;
import com.tem.be.api.dto.dashboard.DepartmentTotalDTO;
import com.tem.be.api.model.WirelessReports;
import com.tem.be.api.service.WirelessReportsService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * REST controller for managing wireless reports related operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/wirelessReports")
public class WirelessReportsController {

    private final WirelessReportsService wirelessReportsService;

    /**
     * @param wirelessReportsService the service handling wireless reports operations
     */
    @Autowired
    public WirelessReportsController(WirelessReportsService wirelessReportsService) {
        this.wirelessReportsService = wirelessReportsService;
    }

    /**
     * Returns all wireless reports.
     */
    @GetMapping(value = "/getAllWirelessReports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<WirelessReports>>> getAllTelecomReports() {
        log.info("WirelessReportsController.getAllWirelessReports() >> Entered");
        List<WirelessReports> wReports = wirelessReportsService.getAllWirelessReports();
        ApiResponse<List<WirelessReports>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, wReports);
        log.info("WirelessReportsController.getAllWirelessReports() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns distinct department and carrier combinations.
     */
    @GetMapping(value = "/getDistinctDepartmentsAndCarriers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DepartmentCarrierDistinctDTO>> getDistinctDepartmentsAndCarriers() {
        log.info("WirelessReportsController.getDistinctDepartmentsAndCarriers() >> Entered");

        DepartmentCarrierDistinctDTO result = wirelessReportsService.getDistinctDepartmentAndCarrier();
        ApiResponse<DepartmentCarrierDistinctDTO> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, result);

        log.info("WirelessReportsController.getDistinctDepartmentsAndCarriers() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Updates a wireless report by ID.
     *
     * @param id  report ID
     * @param dto updated report details
     */
    @PutMapping(value = "/updateById/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WirelessReports> updateById(@PathVariable("id") Long id, @RequestBody ReportUpdateDTO dto) {
        log.info("WirelessReportsController.updateById() >> Entered");

        WirelessReports updateTelecom = wirelessReportsService.updateById(id, dto);

        log.info("WirelessReportsController.updateById() >> Exited");
        return new ResponseEntity<>(updateTelecom, HttpStatus.OK);
    }

    /**
     * Filters wireless reports by departments and carriers.
     *
     * @param departments optional list of departments
     * @param carriers    optional list of carriers
     */
    @GetMapping(value = "/filterWirelessReports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<WirelessReports>>> filterWirelessReports(
            @RequestParam(required = false) List<String> departments,
            @RequestParam(required = false) List<String> carriers) {

        log.info("WirelessReportsController.filterWirelessReports() >> Entered");
        List<WirelessReports> filteredReports = wirelessReportsService.filterWirelessReports(departments, carriers);
        ApiResponse<List<WirelessReports>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, filteredReports);

        log.info("WirelessReportsController.filterWirelessReports() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns total reports grouped by department.
     */
    @GetMapping(value = "/totalByDepartment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<TotalByDepartmentDTO>>> getTotalByDepartment() {
        log.info("WirelessReportsController.getTotalByDepartment() >> Entered");

        List<TotalByDepartmentDTO> result = wirelessReportsService.getTotalByDepartment();
        ApiResponse<List<TotalByDepartmentDTO>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, result);

        log.info("WirelessReportsController.getTotalByDepartment() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns total reports grouped by carrier.
     */
    @GetMapping(value = "/totalByCarrier", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<TotalByCarrierDTO>>> totalByCarrier() {
        log.info("WirelessReportsController.totalByCarrier() >> Entered");

        List<TotalByCarrierDTO> result = wirelessReportsService.totalByCarrier();
        ApiResponse<List<TotalByCarrierDTO>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, result);

        log.info("WirelessReportsController.totalByCarrier() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns wireless reports between two dates.
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     */
    @GetMapping(value = "/reportsByDateRange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<WirelessReports>>> getReportsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate
    ) {
        log.info("WirelessReportsController.getReportsByDateRange() >> Entered");
        List<WirelessReports> reports = wirelessReportsService.getReportsBetweenDates(startDate, endDate);
        ApiResponse<List<WirelessReports>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, reports);
        log.info("WirelessReportsController.getReportsByDateRange() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns carrier plans grouped by department.
     */
    @GetMapping(value = "/departmentWiseCarrierPlans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<DepartmentCarrierPlansDTO>>> getDepartmentWiseCarrierPlans() {
        log.info("WirelessReportsController.getDepartmentWiseCarrierPlans() >> Entered");
        List<DepartmentCarrierPlansDTO> res = wirelessReportsService.getDepartmentWiseCarrierPlans();
        ApiResponse<List<DepartmentCarrierPlansDTO>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, res);
        log.info("WirelessReportsController.getDepartmentWiseCarrierPlans() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns carrier totals within a date range.
     *
     * @param startDate start of date range
     * @param endDate   end of date range
     */
    @GetMapping(value = "/carrierTotalsByRange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<DepartmentTotalDTO>>> getCarrierTotalsByRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate
    ) {
        log.info("WirelessReportsController.getCarrierTotalsByRange() >> Entered");
        List<DepartmentTotalDTO> reports = wirelessReportsService.getCarrierTotalsByRange(startDate, endDate);
        ApiResponse<List<DepartmentTotalDTO>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, reports);
        log.info("WirelessReportsController.getCarrierTotalsByRange() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Returns filtered wireless reports by optional date range, department, and carrier.
     *
     * @param startDate  optional start date
     * @param endDate    optional end date
     * @param department optional department
     * @param carrier    optional carrier
     */
    @GetMapping(value = "/reportsByFilter", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<WirelessReports>>> getReportsByFilter(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "carrier", required = false) String carrier
    ) {
        log.info("WirelessReportsController.getReportsByFilter() >> Entered");
        List<WirelessReports> reports = wirelessReportsService.getFilteredReports(startDate, endDate, department, carrier);
        ApiResponse<List<WirelessReports>> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, reports);
        log.info("WirelessReportsController.getReportsByFilter() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a summary of expenses grouped by department for a specific carrier and date range.
     *
     * @param carrier   The network provider, such as 'firstnet', 'att', or 'verizon'.
     * @param startDate The start date of the reporting period (inclusive). Expected format: YYYY-MM-DD.
     * @param endDate   The end date of the reporting period (inclusive). Expected format: YYYY-MM-DD.
     * @return A {@link ResponseEntity} containing an {@link ApiResponse} with the structured expense summary.
     */
    @GetMapping(value = "/expenseSummaryByCarrier", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<DepartmentExpenseDTO>>> getExpenseSummaryByCarrier(
            @RequestParam("carrier") String carrier,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {

        log.info("WirelessReportsController.getExpenseSummaryByCarrier() >> Entered for carrier: {}", carrier);

        List<DepartmentExpenseDTO> summary = wirelessReportsService.getExpenseSummaryByCarrier(carrier, startDate, endDate);
        ApiResponse<List<DepartmentExpenseDTO>> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", summary);

        log.info("WirelessReportsController.getExpenseSummaryByCarrier() >> Exited");
        return ResponseEntity.ok(response);
    }
}
