package com.tem.be.api.controller;

import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InventoryFilterDto;
import com.tem.be.api.model.Inventoryable;
import com.tem.be.api.service.InventoryUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the {@link InventoryUploadController}.
 * <p>
 * This class uses {@link MockMvc} to simulate HTTP requests to the controller's endpoints.
 * The service layer, {@link InventoryUploadService}, is mocked using {@link MockBean}
 * to isolate the controller logic for unit testing. Each test verifies a specific endpoint's
 * behavior, including success cases, error handling, and expected HTTP responses.
 */
@WebMvcTest(InventoryUploadController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security filters for simpler testing
class InventoryUploadControllerTest {

    /**
     * MockMvc instance for performing HTTP requests against the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mocked dependency of the controller to simulate service layer behavior.
     */
    @MockBean
    private InventoryUploadService inventoryUploadService;

    // Test file objects reused across multiple test cases
    private MockMultipartFile csvFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile invalidTypeFile;

    /**
     * Initializes common test data before each test method runs.
     * This ensures a clean and consistent state for every test, following the
     * Arrange-Act-Assert pattern.
     */
    @BeforeEach
    void setUp() {
        csvFile = new MockMultipartFile("file", "inventory.csv", "text/csv", "header1,header2\nvalue1,value2".getBytes());
        emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        invalidTypeFile = new MockMultipartFile("file", "inventory.txt", "text/plain", "some text".getBytes());
    }

    /**
     * Tests the successful upload of a valid inventory file.
     * It verifies that the endpoint returns HTTP status 200 OK and a response containing the batch ID.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload - Should process valid file and return 200 OK with Batch ID")
    void uploadInventoryFile_withValidFile_shouldReturnOkWithBatchId() throws Exception {
        // Arrange
        String batchId = UUID.randomUUID().toString();
        given(inventoryUploadService.processInventoryFile(any(MockMultipartFile.class), eq("FirstNet"), eq("testUser")))
                .willReturn(batchId);

        // Act & Assert
        mockMvc.perform(multipart("/inventory/upload")
                        .file(csvFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")))
                .andExpect(jsonPath("$.data", is("File processed successfully and data is ready for review. Batch ID: " + batchId)));
    }

    /**
     * Tests the handling of an empty file upload.
     * It verifies that the endpoint's validation works correctly and returns HTTP status 400 Bad Request.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload - Should reject empty file and return 400 Bad Request")
    void uploadInventoryFile_withEmptyFile_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/inventory/upload")
                        .file(emptyFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.data", is("Uploaded file is empty.")));
    }

    /**
     * Tests the handling of a file with an invalid extension.
     * It verifies that the endpoint's validation works correctly and returns HTTP status 400 Bad Request.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload - Should reject invalid file type and return 400 Bad Request")
    void uploadInventoryFile_withInvalidType_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/inventory/upload")
                        .file(invalidTypeFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.data", is("Invalid file type. Please upload a CSV or XLSX file.")));
    }

    /**
     * Tests the error handling when the service layer throws an exception during file processing.
     * It verifies that the endpoint returns HTTP status 400 Bad Request with a specific error message.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload - Should return 400 Bad Request when service throws exception")
    void uploadInventoryFile_whenServiceThrowsException_shouldReturnBadRequest() throws Exception {
        // Arrange
        String errorMessage = "File headers are invalid";
        doThrow(new RuntimeException(errorMessage))
                .when(inventoryUploadService).processInventoryFile(any(MockMultipartFile.class), eq("FirstNet"), eq("testUser"));

        // Act & Assert
        mockMvc.perform(multipart("/inventory/upload")
                        .file(csvFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.status", is("Processing Error")))
                .andExpect(jsonPath("$.data", is(errorMessage)));
    }

    /**
     * Tests the successful retrieval of distinct departments for a carrier.
     * It verifies that the endpoint returns HTTP status 200 OK and the list of departments.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /distinct-departments/{carrier} - Should return departments and 200 OK")
    void getDistinctDepartments_shouldReturnDepartments() throws Exception {
        // Arrange
        String carrier = "firstnet";
        List<String> departments = List.of("Dept A", "Dept B");
        DepartmentDistinctDTO dto = new DepartmentDistinctDTO(departments);
        given(inventoryUploadService.getDistinctDepartments(carrier)).willReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/inventory/distinct-departments/{carrier}", carrier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.data.departments", hasSize(2)))
                .andExpect(jsonPath("$.data.departments[0]", is("Dept A")));
    }

    /**
     * Tests the successful search for inventories with pagination.
     * It verifies that the endpoint returns HTTP status 200 OK and a paginated list of results.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /search - Should return paginated results and 200 OK")
    void searchInventories_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        String carrier = "FirstNet";
        Page<Inventoryable> pagedResponse = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        given(inventoryUploadService.searchInventories(eq(carrier), any(InventoryFilterDto.class), any(Pageable.class)))
                .willReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/inventory/search")
                        .param("carrier", carrier)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    /**
     * Tests the error handling when the search service throws an exception.
     * It verifies that the endpoint returns HTTP status 400 Bad Request for an unsupported carrier.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /search - Should return 400 Bad Request for unsupported carrier")
    void searchInventories_whenServiceThrowsIllegalArgument_shouldReturnBadRequest() throws Exception {
        // Arrange
        String carrier = "UnsupportedCarrier";
        String errorMessage = "Unsupported carrier for filtering: " + carrier;
        given(inventoryUploadService.searchInventories(eq(carrier), any(InventoryFilterDto.class), any(Pageable.class)))
                .willThrow(new IllegalArgumentException(errorMessage));

        // Act & Assert
        mockMvc.perform(get("/inventory/search")
                        .param("carrier", carrier)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.status", is("Bad Request")))
                .andExpect(jsonPath("$.data", is(errorMessage)));
    }
}
