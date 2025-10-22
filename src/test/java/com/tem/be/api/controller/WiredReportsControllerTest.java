package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.UpdateViscodeRequest;
import com.tem.be.api.dto.WiredReportsFilterDto;
import com.tem.be.api.model.WiredReports;
import com.tem.be.api.service.WiredReportsService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link WiredReportsController}.
 * <p>
 * This class uses {@link MockMvc} to simulate HTTP requests to the controller's endpoints.
 * The service layer, {@link WiredReportsService}, is mocked using {@link MockBean}
 * to isolate the controller logic for unit testing. Each test verifies a specific endpoint's
 * behavior and the expected HTTP responses, following the Arrange-Act-Assert pattern.
 */
@WebMvcTest(WiredReportsController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security filters for simpler testing
class WiredReportsControllerTest {

    /**
     * MockMvc instance for performing HTTP requests against the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mocked dependency of the controller to simulate service layer behavior.
     */
    @MockBean
    private WiredReportsService wiredReportsService;

    /**
     * Jackson's ObjectMapper for serializing Java objects into JSON strings.
     */
    @Autowired
    private ObjectMapper objectMapper;

    private WiredReports report1;
    private UpdateViscodeRequest updateViscodeRequest;

    /**
     * Initializes common test data before each test method runs.
     * This ensures a clean and consistent state for every test.
     */
    @BeforeEach
    void setUp() {
        report1 = new WiredReports();
        report1.setId(1L);
        report1.setCarrier("CALNET");
        report1.setViscode("V100");

        updateViscodeRequest = new UpdateViscodeRequest();
        updateViscodeRequest.setNewViscode("V250");
        updateViscodeRequest.setCarrier("CALNET");
    }

    /**
     * Tests the successful retrieval of wired reports based on filter criteria including a date range.
     * It verifies that the endpoint returns HTTP status 200 OK and a filtered list of reports.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /wiredReports/searchWiredReports - Should return filtered reports for carrier and date range")
    void searchWiredReports_shouldReturnFilteredReports() throws Exception {
        // Arrange
        when(wiredReportsService.searchWiredReports(any(WiredReportsFilterDto.class)))
                .thenReturn(List.of(report1));

        String carrier = "CALNET";
        String startDate = "2024-05-01";
        String endDate = "2024-05-31";

        // Act & Assert
        mockMvc.perform(get("/wiredReports/searchWiredReports")
                        .param("carrier", carrier)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[0].carrier", is("CALNET")));
    }

    /**
     * Tests the successful partial update of a wired report's viscode.
     * It verifies that the PATCH endpoint returns HTTP status 200 OK and the updated report object.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("PATCH /wiredReports/{id}/viscode - Should update viscode and return status 200 OK")
    void updateViscode_shouldUpdateAndReturnReport() throws Exception {
        // Arrange
        Long reportId = 1L;
        WiredReports updatedReport = new WiredReports();
        updatedReport.setId(reportId);
        updatedReport.setCarrier("CALNET");
        updatedReport.setViscode(updateViscodeRequest.getNewViscode());

        when(wiredReportsService.updateViscode(
                reportId,
                updateViscodeRequest.getNewViscode(),
                updateViscodeRequest.getCarrier()
        )).thenReturn(updatedReport);

        // Act & Assert
        mockMvc.perform(patch("/wiredReports/{id}/viscode", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateViscodeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Successfully updated viscode.")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.viscode", is("V250")));
    }
}