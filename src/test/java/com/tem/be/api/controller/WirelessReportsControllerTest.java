package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.TotalByCarrierDTO;
import com.tem.be.api.dto.TotalByDepartmentDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierPlansDTO;
import com.tem.be.api.dto.dashboard.DepartmentExpenseDTO;
import com.tem.be.api.dto.dashboard.DepartmentTotalDTO;
import com.tem.be.api.model.WirelessReports;
import com.tem.be.api.service.WirelessReportsService;
import com.tem.be.api.utils.RestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link WirelessReportsController}.
 * <p>
 * This class uses {@link MockMvc} to simulate HTTP requests to the controller's endpoints.
 * The service layer, {@link WirelessReportsService}, is mocked using {@link MockBean}
 * to isolate the controller logic for unit testing. Each test verifies a specific endpoint's
 * behavior and the expected HTTP responses, following the Arrange-Act-Assert pattern.
 */
@WebMvcTest(WirelessReportsController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security filters for simpler testing
class WirelessReportsControllerTest {

    /**
     * MockMvc instance for performing HTTP requests against the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mocked dependency of the controller to simulate service layer behavior.
     */
    @MockBean
    private WirelessReportsService wirelessReportsService;

    /**
     * Jackson's ObjectMapper for serializing Java objects into JSON strings.
     */
    @Autowired
    private ObjectMapper objectMapper;

    // Test data objects reused across multiple test cases
    private WirelessReports report1;
    private WirelessReports report2;
    private ReportUpdateDTO reportUpdateDTO;

    /**
     * SimpleDateFormat for parsing and formatting date parameters in requests.
     */
    private SimpleDateFormat sdf;

    /**
     * Initializes common test data before each test method runs.
     * This ensures a clean and consistent state for every test.
     */
    @BeforeEach
    void setUp() {
        report1 = new WirelessReports();
        report1.setId(1L);
        report1.setDepartment("IT");
        report1.setCarrier("Verizon");

        report2 = new WirelessReports();
        report2.setId(2L);
        report2.setDepartment("Sales");
        report2.setCarrier("AT&T Mobility");

        reportUpdateDTO = new ReportUpdateDTO();
        reportUpdateDTO.setDepartment("Finance");
        reportUpdateDTO.setViscode("V123");
        reportUpdateDTO.setNameOnInvoice("John Doe");

        // Standard ISO 8601 format for date/time with offset, used in controller parameters
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    /**
     * Tests the successful retrieval of all wireless reports.
     * It verifies that the endpoint returns HTTP status 200 OK and a JSON array
     * containing all reports.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/getAllWirelessReports - Should return all reports and status 200 OK")
    void getAllWirelessReports_shouldReturnAllReports() throws Exception {
        // Arrange
        when(wirelessReportsService.getAllWirelessReports()).thenReturn(List.of(report1, report2));

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/getAllWirelessReports"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].department", is("IT")));
    }

    /**
     * Tests the successful retrieval of distinct departments, carriers, and device types.
     * It verifies that the endpoint returns HTTP status 200 OK and a DTO with lists.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/getDistinctDepartmentsAndCarriers - Should return distinct values and status 200 OK")
    void getDistinctDepartmentsAndCarriers_shouldReturnDistinctValues() throws Exception {
        // Arrange
        DepartmentCarrierDistinctDTO distinctDTO = new DepartmentCarrierDistinctDTO(
                List.of("IT", "Sales"), List.of("Verizon", "AT&T Mobility"), List.of("Phone", "Tablet")
        );
        when(wirelessReportsService.getDistinctDepartmentAndCarrier()).thenReturn(distinctDTO);

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/getDistinctDepartmentsAndCarriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.departments", hasSize(2)))
                .andExpect(jsonPath("$.data.carriers[0]", is("Verizon")));
    }

    /**
     * Tests the successful update of a wireless report by its ID.
     * It verifies that the endpoint returns HTTP status 200 OK and the updated report object.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("PUT /wirelessReports/updateById/{id} - Should update a report and return status 200 OK")
    void updateById_shouldReturnUpdatedReport() throws Exception {
        // Arrange
        Long reportId = 1L;
        WirelessReports updatedReport = new WirelessReports();
        updatedReport.setId(reportId);
        updatedReport.setDepartment(reportUpdateDTO.getDepartment());

        when(wirelessReportsService.updateById(eq(reportId), any(ReportUpdateDTO.class))).thenReturn(updatedReport);

        // Act & Assert
        mockMvc.perform(put("/wirelessReports/updateById/{id}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.department", is("Finance")));
    }

    /**
     * Tests the successful retrieval of wireless reports based on department and carrier filters.
     * It verifies that the endpoint returns HTTP status 200 OK and a filtered list.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/filterWirelessReports - Should return filtered reports and status 200 OK")
    void filterWirelessReports_shouldReturnFilteredList() throws Exception {
        // Arrange
        when(wirelessReportsService.filterWirelessReports(List.of("IT"), List.of("Verizon"))).thenReturn(List.of(report1));

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/filterWirelessReports")
                        .param("departments", "IT")
                        .param("carriers", "Verizon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)));
    }

    /**
     * Tests the successful retrieval of total expenses grouped by department.
     * It verifies that the endpoint returns HTTP status 200 OK and a list of {@link TotalByDepartmentDTO}.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/totalByDepartment - Should return totals by department")
    void getTotalByDepartment_shouldReturnDepartmentTotals() throws Exception {
        // Arrange
        List<TotalByDepartmentDTO> totals = List.of(new TotalByDepartmentDTO("IT", new BigDecimal("1500.75")));
        when(wirelessReportsService.getTotalByDepartment()).thenReturn(totals);

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/totalByDepartment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("IT")))
                .andExpect(jsonPath("$.data[0].total", is(1500.75)));
    }

    /**
     * Tests the successful retrieval of total expenses grouped by carrier.
     * It verifies that the endpoint returns HTTP status 200 OK and a list of {@link TotalByCarrierDTO}.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/totalByCarrier - Should return totals by carrier")
    void totalByCarrier_shouldReturnCarrierTotals() throws Exception {
        // Arrange
        List<TotalByCarrierDTO> totals = List.of(new TotalByCarrierDTO("Verizon", new BigDecimal("2500.50")));
        when(wirelessReportsService.totalByCarrier()).thenReturn(totals);

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/totalByCarrier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("Verizon"))) // Note: DTO field is 'department' but holds carrier name
                .andExpect(jsonPath("$.data[0].total", is(2500.50)));
    }

    /**
     * Tests the successful retrieval of wireless reports that fall within a specified date range.
     * It verifies that the endpoint returns HTTP status 200 OK and a filtered list of reports.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/reportsByDateRange - Should return reports in date range")
    void getReportsByDateRange_shouldReturnReports() throws Exception {
        // Arrange
        Date startDate = sdf.parse("2024-01-01T00:00:00.000+00:00");
        Date endDate = sdf.parse("2024-01-31T23:59:59.000+00:00");

        when(wirelessReportsService.getReportsBetweenDates(any(Date.class), any(Date.class))).thenReturn(List.of(report1));

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/reportsByDateRange")
                        .param("startDate", sdf.format(startDate))
                        .param("endDate", sdf.format(endDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)));
    }

    /**
     * Tests the successful retrieval of carrier plans grouped by department.
     * It verifies that the endpoint returns HTTP status 200 OK and a list of {@link DepartmentCarrierPlansDTO}.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/departmentWiseCarrierPlans - Should return carrier plans by department")
    void getDepartmentWiseCarrierPlans_shouldReturnPlans() throws Exception {
        // Arrange
        List<DepartmentCarrierPlansDTO> plans = List.of(new DepartmentCarrierPlansDTO("IT", Collections.emptyList(), 5));
        when(wirelessReportsService.getDepartmentWiseCarrierPlans()).thenReturn(plans);

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/departmentWiseCarrierPlans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is(RestConstants.SUCCESS_STRING)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("IT")))
                .andExpect(jsonPath("$.data[0].totalPlans", is(5)));
    }

    /**
     * Tests the successful retrieval of carrier totals aggregated within a specific date range.
     * It verifies that the endpoint returns HTTP status 200 OK and a list of {@link DepartmentTotalDTO}.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/carrierTotalsByRange - Should return carrier totals in a date range")
    void getCarrierTotalsByRange_shouldReturnTotals() throws Exception {
        // Arrange
        Date startDate = sdf.parse("2024-01-01T00:00:00.000+00:00");
        Date endDate = sdf.parse("2024-01-31T23:59:59.000+00:00");
        List<DepartmentTotalDTO> totals = List.of(new DepartmentTotalDTO("IT", Collections.emptyList(), new BigDecimal("123.45")));
        when(wirelessReportsService.getCarrierTotalsByRange(any(Date.class), any(Date.class))).thenReturn(totals);

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/carrierTotalsByRange")
                        .param("startDate", sdf.format(startDate))
                        .param("endDate", sdf.format(endDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("IT")))
                .andExpect(jsonPath("$.data[0].total", is(123.45)));
    }

    /**
     * Tests the successful retrieval of reports filtered by date range, department, and carrier.
     * It verifies that the endpoint returns HTTP status 200 OK and a filtered list of reports.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/reportsByFilter - Should return filtered reports")
    void getReportsByFilter_shouldReturnFilteredReports() throws Exception {
        // Arrange
        Date startDate = sdf.parse("2024-01-01T00:00:00.000+00:00");
        Date endDate = sdf.parse("2024-01-31T23:59:59.000+00:00");
        String department = "IT";
        String carrier = "Verizon";
        when(wirelessReportsService.getFilteredReports(any(), any(), eq(department), eq(carrier))).thenReturn(List.of(report1));

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/reportsByFilter")
                        .param("startDate", sdf.format(startDate))
                        .param("endDate", sdf.format(endDate))
                        .param("department", department)
                        .param("carrier", carrier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("IT")));
    }


    /**
     * Tests the successful retrieval of the expense summary by a specific carrier and date range.
     * It verifies that the endpoint returns HTTP status 200 OK and a list of {@link DepartmentExpenseDTO}.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wirelessReports/expenseSummaryByCarrier - Should return expense summary and status 200 OK")
    void getExpenseSummaryByCarrier_shouldReturnSummary() throws Exception {
        // Arrange
        String carrier = "firstnet";
        String startDateStr = "2024-01-01";
        String endDateStr = "2024-01-31";
        List<DepartmentExpenseDTO> summary = Collections.singletonList(new DepartmentExpenseDTO("IT", Collections.emptyList(), null));
        when(wirelessReportsService.getExpenseSummaryByCarrier(eq(carrier), any(Date.class), any(Date.class))).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/wirelessReports/expenseSummaryByCarrier")
                        .param("carrier", carrier)
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("IT")));
    }
}