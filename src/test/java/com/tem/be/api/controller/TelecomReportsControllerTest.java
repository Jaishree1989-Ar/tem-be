package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.model.TelecomReports;
import com.tem.be.api.service.TelecomReportsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TelecomReportsController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelecomReportsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelecomReportsService telecomReportsService;

    @Autowired
    private ObjectMapper objectMapper;

    private TelecomReports report1;
    private ReportUpdateDTO reportUpdateDTO;

    @BeforeEach
    void setUp() {
        report1 = new TelecomReports();
        report1.setId(1L);
        report1.setDepartment("Sales");
        report1.setCarrier("Verizon");

        reportUpdateDTO = new ReportUpdateDTO();
        reportUpdateDTO.setDepartment("Marketing");
        reportUpdateDTO.setViscode("V456");
        reportUpdateDTO.setNameOnInvoice("Jane Smith");
    }

    /**
     * Tests the GET endpoint for retrieving all telecom reports.
     * It verifies the HTTP status is 200 OK, the content type is JSON,
     * and the response body structure and data are correct.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /telecomReports/getAllTelecomReports - Should return list of reports and status 200 OK")
    void getAllTelecomReports_shouldReturnListOfReports() throws Exception {
        // Arrange
        when(telecomReportsService.getAllTelecomReports()).thenReturn(List.of(report1));

        // Act & Assert
        mockMvc.perform(get("/telecomReports/getAllTelecomReports"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].department", is("Sales")));
    }

    /**
     * Tests the PUT endpoint for updating a telecom report by its ID.
     * It verifies the HTTP status is 200 OK and the returned report
     * contains the updated values.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("PUT /telecomReports/updateById/{id} - Should update and return report with status 200 OK")
    void updateById_shouldReturnUpdatedReport() throws Exception {
        // Arrange
        TelecomReports updatedReport = new TelecomReports();
        updatedReport.setId(1L);
        updatedReport.setDepartment(reportUpdateDTO.getDepartment());
        updatedReport.setViscode(reportUpdateDTO.getViscode());

        when(telecomReportsService.updateById(eq(1L), any(ReportUpdateDTO.class))).thenReturn(updatedReport);

        // Act & Assert
        mockMvc.perform(put("/telecomReports/updateById/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.department", is("Marketing")));
    }

    /**
     * Tests the GET endpoint for retrieving distinct department and carrier values.
     * It verifies the HTTP status is 200 OK and checks the size and content
     * of the returned distinct values DTO.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /telecomReports/getDistinctDepartmentsAndCarriers - Should return distinct values")
    void getDistinctDepartmentsAndCarriers_shouldReturnDto() throws Exception {
        // Arrange
        DepartmentCarrierDistinctDTO distinctDTO = new DepartmentCarrierDistinctDTO(
                List.of("Sales", "HR"), List.of("Verizon", "Comcast"), List.of("Desk Phone")
        );
        when(telecomReportsService.getDistinctDepartmentAndCarrier()).thenReturn(distinctDTO);

        // Act & Assert
        mockMvc.perform(get("/telecomReports/getDistinctDepartmentsAndCarriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.departments", hasSize(2)))
                .andExpect(jsonPath("$.data.carriers[1]", is("Comcast")));
    }

    /**
     * Tests the GET endpoint for filtering telecom reports by department and carrier.
     * It verifies the HTTP status is 200 OK and checks if the filtered list
     * contains the expected report.
     *
     * @throws Exception if the MockMvc request fails
     */
    @Test
    @DisplayName("GET /telecomReports/filterTelecomReports - Should return filtered list of reports")
    void filterTelecomReports_shouldReturnFilteredReports() throws Exception {
        // Arrange
        List<String> departments = List.of("Sales");
        List<String> carriers = List.of("Verizon");
        when(telecomReportsService.telecomReportsService(departments, carriers)).thenReturn(List.of(report1));

        // Act & Assert
        mockMvc.perform(get("/telecomReports/filterTelecomReports")
                        .param("departments", "Sales")
                        .param("carriers", "Verizon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].carrier", is("Verizon")));
    }
}