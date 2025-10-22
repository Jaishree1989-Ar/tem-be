package com.tem.be.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InvoiceFilterDto;
import com.tem.be.api.dto.UpdateRecurringChargesRequest;
import com.tem.be.api.model.FirstNetInvoice;
import com.tem.be.api.model.Invoiceable;
import com.tem.be.api.service.InvoiceUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the {@link InvoiceUploadController}.
 * <p>
 * This class uses {@link MockMvc} to simulate HTTP requests to the controller's endpoints.
 * The service layer, {@link InvoiceUploadService}, is mocked using {@link MockBean}
 * to isolate the controller logic for unit testing. Each test verifies a specific endpoint's
 * behavior, including success cases, error handling, and expected HTTP responses.
 */
@WebMvcTest(InvoiceUploadController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables security filters for simpler testing
class InvoiceUploadControllerTest {

    /**
     * MockMvc instance for performing HTTP requests against the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mocked dependency of the controller to simulate service layer behavior.
     */
    @MockBean
    private InvoiceUploadService invoiceUploadService;

    /**
     * Jackson's ObjectMapper for serializing Java objects into JSON for request bodies.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test file objects reused across multiple test cases.
     */
    private MockMultipartFile csvFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile invalidTypeFile;

    /**
     * Initializes common test data before each test method runs.
     * This ensures a clean and consistent state for every test, promoting test isolation.
     * It follows the Arrange part of the Arrange-Act-Assert pattern.
     */
    @BeforeEach
    void setUp() {
        csvFile = new MockMultipartFile("file", "invoice.csv", "text/csv", "header,data\nvalue,1".getBytes());
        emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);
        invalidTypeFile = new MockMultipartFile("file", "invoice.txt", "text/plain", "some data".getBytes());
    }

    /**
     * Tests the successful upload of a valid invoice detail file.
     * It verifies that the endpoint returns HTTP status 200 OK and a response containing the generated batch ID.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload-details - Should process valid file and return 200 OK with Batch ID")
    void uploadDetailFile_withValidFile_shouldReturnOk() throws Exception {
        // Arrange
        String batchId = UUID.randomUUID().toString();
        given(invoiceUploadService.processDetailFile(any(MockMultipartFile.class), eq("FirstNet"), eq("testUser")))
                .willReturn(batchId);

        // Act & Assert
        mockMvc.perform(multipart("/invoice/upload-details")
                        .file(csvFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")))
                .andExpect(jsonPath("$.data", is("File processed successfully and data is ready for review. Batch ID: " + batchId)));
    }

    /**
     * Tests the controller's validation logic for an empty file upload.
     * It verifies that a 400 Bad Request is returned without calling the service layer.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload-details - Should reject empty file and return 400 Bad Request")
    void uploadDetailFile_withEmptyFile_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/invoice/upload-details")
                        .file(emptyFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.data", is("Uploaded file is empty.")));
    }

    /**
     * Tests the controller's validation logic for a file with an unsupported extension.
     * It verifies that a 400 Bad Request is returned without calling the service layer.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload-details - Should reject invalid file type and return 400 Bad Request")
    void uploadDetailFile_withInvalidType_shouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/invoice/upload-details")
                        .file(invalidTypeFile)
                        .param("carrier", "FirstNet")
                        .param("uploadedBy", "testUser"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.data", is("Invalid file type. Please upload a CSV or XLSX file.")));
    }

    /**
     * Tests the error handling when the service layer throws an exception during file processing.
     * It verifies the endpoint catches the exception and returns a 400 Bad Request with the error message.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("POST /upload-details - Should return 400 Bad Request when service throws exception")
    void uploadDetailFile_whenServiceFails_shouldReturnBadRequest() throws Exception {
        // Arrange
        String errorMessage = "Required columns are missing from the file.";
        given(invoiceUploadService.processDetailFile(any(), any(), any()))
                .willThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(multipart("/invoice/upload-details")
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
     * It verifies the endpoint returns a 200 OK status and a JSON object containing the list of departments.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /distinct-departments/{carrier} - Should return departments and 200 OK")
    void getDistinctDepartmentsByCarrier_shouldReturnDepartments() throws Exception {
        // Arrange
        String carrier = "FirstNet";
        DepartmentDistinctDTO dto = new DepartmentDistinctDTO(List.of("Sales", "Marketing"));
        given(invoiceUploadService.getDistinctDepartments(carrier)).willReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/invoice/distinct-departments/{carrier}", carrier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.data.departments", hasSize(2)))
                .andExpect(jsonPath("$.data.departments[0]", is("Sales")));
    }

    /**
     * Tests the successful search for invoices with pagination.
     * It verifies that the endpoint returns a 200 OK status and a paginated list of results.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("GET /search - Should return paginated results and 200 OK")
    void searchInvoices_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        String carrier = "FirstNet";
        Page<Invoiceable> pagedResponse = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        given(invoiceUploadService.searchInvoices(eq(carrier), any(InvoiceFilterDto.class), any(Pageable.class)))
                .willReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/invoice/search")
                        .param("carrier", carrier)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.status", is("Success")))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    /**
     * Tests the successful update of an invoice's recurring charges.
     * It verifies that the PATCH endpoint returns a 200 OK status and the updated invoice object in the response body.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("PATCH /{id}/recurring-charges - Should update charges and return 200 OK")
    void updateRecurringCharges_shouldReturnUpdatedInvoice() throws Exception {
        // Arrange
        Long invoiceId = 1L;
        String carrier = "FirstNet";
        UpdateRecurringChargesRequest request = new UpdateRecurringChargesRequest();
        request.setCarrier(carrier);
        request.setNewRecurringCharges(new BigDecimal("123.45"));

        FirstNetInvoice updatedInvoice = new FirstNetInvoice();
        updatedInvoice.setInvoiceId(invoiceId);
        updatedInvoice.setTotalReoccurringCharges(new BigDecimal("123.45"));

        given(invoiceUploadService.updateRecurringCharges(invoiceId, new BigDecimal("123.45"), carrier))
                .willReturn(updatedInvoice);

        // Act & Assert
        mockMvc.perform(patch("/invoice/{id}/recurring-charges", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.OK.value())))
                .andExpect(jsonPath("$.status", is("Successfully updated recurring charges.")))
                .andExpect(jsonPath("$.data.invoiceId", is(1)))
                .andExpect(jsonPath("$.data.totalReoccurringCharges", is(123.45)));
    }

    /**
     * Tests the error handling for updating a non-existent invoice.
     * It verifies that when the service throws an {@link EntityNotFoundException}, the controller advice
     * translates this into a 404 Not Found response.
     *
     * @throws Exception if the MockMvc call fails.
     */
    @Test
    @DisplayName("PATCH /{id}/recurring-charges - Should return 404 Not Found if invoice does not exist")
    void updateRecurringCharges_whenInvoiceNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        Long invoiceId = 999L;
        String carrier = "FirstNet";
        UpdateRecurringChargesRequest request = new UpdateRecurringChargesRequest();
        request.setCarrier(carrier);
        request.setNewRecurringCharges(new BigDecimal("100.00"));

        String errorMessage = "FirstNetInvoice not found with ID: " + invoiceId;
        doThrow(new EntityNotFoundException(errorMessage))
                .when(invoiceUploadService).updateRecurringCharges(any(), any(), any());

        // Act & Assert
        mockMvc.perform(patch("/invoice/{id}/recurring-charges", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode", is(HttpStatus.NOT_FOUND.value())))
                .andExpect(jsonPath("$.data", is(errorMessage)));
    }
}
