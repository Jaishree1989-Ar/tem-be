package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.AccountDepartmentMappingDTO;
import com.tem.be.api.model.AccountDepartmentMapping;
import com.tem.be.api.service.AccountDepartmentMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the {@link AccountDepartmentMappingController}.
 * <p>
 * This class uses {@link MockMvc} to simulate HTTP requests to the controller's endpoints.
 * The service layer, {@link AccountDepartmentMappingService}, is mocked using {@link MockBean}
 * to isolate the controller logic for unit testing. Each test verifies a specific endpoint's
 * behavior, including success cases, error handling, and expected HTTP responses.
 */
@WebMvcTest(AccountDepartmentMappingController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security filters for simpler testing
class AccountDepartmentMappingControllerTest {

    /**
     * MockMvc instance for performing HTTP requests against the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mocked dependency of the controller to simulate service layer behavior.
     */
    @MockBean
    private AccountDepartmentMappingService mappingService;

    /**
     * Jackson's ObjectMapper for serializing Java objects into JSON strings.
     */
    @Autowired
    private ObjectMapper objectMapper;

    // Test data objects reused across multiple test cases
    private AccountDepartmentMapping mapping1;
    private AccountDepartmentMapping mapping2;
    private AccountDepartmentMappingDTO mappingDTO;

    /**
     * Initializes common test data before each test method runs.
     * This ensures a clean and consistent state for every test, following the
     * Arrange-Act-Assert pattern.
     */
    @BeforeEach
    void setUp() {
        // Common test data initialized before each test
        mapping1 = new AccountDepartmentMapping();
        mapping1.setId(1L);
        mapping1.setCarrier("AT&T Mobility");
        mapping1.setDepartment("Sales");
        mapping1.setDepartmentAccountNumber("ACC123");

        mapping2 = new AccountDepartmentMapping();
        mapping2.setId(2L);
        mapping2.setCarrier("AT&T Mobility");
        mapping2.setDepartment("Marketing");
        mapping2.setDepartmentAccountNumber("ACC456");

        mappingDTO = new AccountDepartmentMappingDTO();
        mappingDTO.setCarrier("AT&T Mobility");
        mappingDTO.setDepartment("Engineering");
        mappingDTO.setDepartmentAccountNumber("ACC789");
    }

    /**
     * Tests the successful retrieval of mappings for a specific carrier.
     * It verifies that the endpoint returns HTTP status 200 OK and a JSON array
     * of mappings.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /mappings/carrier/{carrier} - Should return list of mappings and status 200 OK")
    void getMappingsByCarrier_shouldReturnMappings() throws Exception {
        // Arrange
        String carrier = "AT&T Mobility";
        when(mappingService.getMappingsByCarrier(carrier)).thenReturn(List.of(mapping1, mapping2));

        // Act & Assert
        mockMvc.perform(get("/mappings/carrier/{carrier}", carrier))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].department", is("Sales")));
    }

    /**
     * Tests the error handling when the service layer throws an exception during mapping retrieval.
     * It verifies that the endpoint returns HTTP status 500 Internal Server Error.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /mappings/carrier/{carrier} - Should return status 500 when service throws exception")
    void getMappingsByCarrier_whenServiceFails_shouldReturn500() throws Exception {
        // Arrange
        String carrier = "FAIL_CARRIER";
        when(mappingService.getMappingsByCarrier(carrier)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/mappings/carrier/{carrier}", carrier))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode", is(500)))
                .andExpect(jsonPath("$.status", is("Error fetching mappings: Database connection failed")));
    }

    /**
     * Tests the successful creation of a new account-department mapping.
     * It verifies that the endpoint returns HTTP status 201 Created and the newly created resource.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /mappings - Should create a new mapping and return status 201 Created")
    void createMapping_shouldReturnCreatedMapping() throws Exception {
        // Arrange
        AccountDepartmentMapping createdMapping = new AccountDepartmentMapping();
        createdMapping.setId(3L);
        createdMapping.setCarrier(mappingDTO.getCarrier());
        createdMapping.setDepartment(mappingDTO.getDepartment());
        createdMapping.setDepartmentAccountNumber(mappingDTO.getDepartmentAccountNumber());

        when(mappingService.createMapping(any(AccountDepartmentMappingDTO.class))).thenReturn(createdMapping);

        // Act & Assert
        mockMvc.perform(post("/mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mappingDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.status", is("Mapping created successfully")))
                .andExpect(jsonPath("$.data.id", is(3)));
    }

    /**
     * Tests the successful update of an existing account-department mapping.
     * It verifies that the endpoint returns HTTP status 200 OK and the updated resource.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("PUT /mappings/{id} - Should update an existing mapping and return status 200 OK")
    void updateMapping_shouldReturnUpdatedMapping() throws Exception {
        // Arrange
        Long mappingId = 1L;
        when(mappingService.updateMapping(eq(mappingId), any(AccountDepartmentMappingDTO.class))).thenReturn(mapping1);

        // Act & Assert
        mockMvc.perform(put("/mappings/{id}", mappingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mappingDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("Mapping updated successfully")))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    /**
     * Tests the successful deletion of a mapping by its ID.
     * It verifies that the endpoint returns HTTP status 200 OK and a success message.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("DELETE /mappings/{id} - Should delete a mapping and return status 200 OK")
    void deleteMapping_shouldReturnSuccessMessage() throws Exception {
        // Arrange
        Long mappingId = 1L;
        doNothing().when(mappingService).deleteMapping(mappingId);

        // Act & Assert
        mockMvc.perform(delete("/mappings/{id}", mappingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.status", is("200 OK")))
                .andExpect(jsonPath("$.data", is("Mapping with ID " + mappingId + " was deleted.")));
    }

    /**
     * Tests the successful upload and processing of a mapping file.
     * It verifies that the multipart endpoint returns HTTP status 201 Created and a success message.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /mappings/upload - Should process file and return status 201 Created")
    void uploadMappings_withValidFile_shouldReturnCreated() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "mappings.csv", "text/csv", "data".getBytes());
        when(mappingService.uploadMappings(any(MultipartFile.class), eq("testUser"), eq("AT&T Mobility")))
                .thenReturn(List.of(mapping1));

        // Act & Assert
        mockMvc.perform(multipart("/mappings/upload")
                        .file(file)
                        .param("uploadedBy", "testUser")
                        .param("carrier", "AT&T Mobility"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.status", is("Successfully uploaded and processed 1 new mappings.")));
    }

    /**
     * Tests error handling for a file upload when the service throws an {@link IllegalArgumentException}.
     * It verifies that the endpoint returns HTTP status 400 Bad Request.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /mappings/upload - Should return 400 Bad Request for invalid argument")
    void uploadMappings_whenServiceThrowsIllegalArgument_shouldReturnBadRequest() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        when(mappingService.uploadMappings(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("File name cannot be empty."));

        // Act & Assert
        mockMvc.perform(multipart("/mappings/upload")
                        .file(file)
                        .param("uploadedBy", "testUser")
                        .param("carrier", "AT&T Mobility"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(400)))
                .andExpect(jsonPath("$.status", is("File name cannot be empty.")));
    }

    /**
     * Tests error handling for a file upload when the service throws an {@link IOException}.
     * It verifies that the endpoint returns HTTP status 500 Internal Server Error.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /mappings/upload - Should return 500 Internal Server Error for IOException")
    void uploadMappings_whenServiceThrowsIOException_shouldReturnInternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "good_name.csv", "text/csv", "content".getBytes());
        when(mappingService.uploadMappings(any(), any(), any()))
                .thenThrow(new IOException("Disk is full"));

        // Act & Assert
        mockMvc.perform(multipart("/mappings/upload")
                        .file(file)
                        .param("uploadedBy", "testUser")
                        .param("carrier", "AT&T Mobility"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode", is(500)))
                .andExpect(jsonPath("$.status", is("Failed to process file due to a server error.")));
    }
}
