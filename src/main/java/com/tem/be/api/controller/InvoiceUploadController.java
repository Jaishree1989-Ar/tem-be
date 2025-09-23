package com.tem.be.api.controller;

import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InvoiceFilterDto;
import com.tem.be.api.dto.UpdateRecurringChargesRequest;
import com.tem.be.api.model.FirstNetInvoice;
import com.tem.be.api.model.Invoiceable;
import com.tem.be.api.service.InvoiceUploadService;
import com.tem.be.api.utils.ApiResponse;
import com.tem.be.api.utils.RestConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Objects;

/**
 * REST controller for managing invoice upload operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/invoice")
public class InvoiceUploadController {

    private final InvoiceUploadService invoiceUploadService;
    private static final String ERROR_STATUS = "Error";

    @Autowired
    public InvoiceUploadController(InvoiceUploadService invoiceUploadService) {
        this.invoiceUploadService = invoiceUploadService;
    }

    /**
     * Gets a list of distinct department names for a specific carrier.
     * The carrier name is passed as a path variable.
     *
     * @param carrier The name of the carrier (e.g., "firstnet", "att").
     * @return A list of distinct departments.
     */
    @GetMapping(value = "/distinct-departments/{carrier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DepartmentDistinctDTO>> getDistinctDepartmentsByCarrier(@PathVariable String carrier) {
        log.info("InvoiceUploadController.getDistinctDepartmentsByCarrier({}) >> Entered", carrier);
        DepartmentDistinctDTO result = invoiceUploadService.getDistinctDepartments(carrier);
        ApiResponse<DepartmentDistinctDTO> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, result);
        log.info("InvoiceUploadController.getDistinctDepartmentsByCarrier({}) >> Exited", carrier);
        return ResponseEntity.ok(response);
    }

    /**
     * Searches for invoices with dynamic filter criteria, pagination, and sorting.
     * The structure of the returned objects depends on the specified 'carrier'.
     *
     * @param carrier  The carrier to search for (e.g., "FirstNet"). Required.
     * @param filter   A DTO containing all optional filter parameters.
     * @param pageable Standard pagination and sorting parameters.
     * @return A paginated list of carrier-specific invoices matching the criteria.
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<Invoiceable>>> searchInvoices(
            @RequestParam String carrier,
            InvoiceFilterDto filter,
            Pageable pageable) {

        log.info("InvoiceController.searchInvoices() >> Entered for carrier: {} with filter: {}", carrier, filter);

        Page<Invoiceable> results = invoiceUploadService.searchInvoices(carrier, filter, pageable);

        ApiResponse<Page<Invoiceable>> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", results);

        log.info("InvoiceController.searchInvoices() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to upload a ZIP file containing invoice PDFs and a corresponding detail report (CSV/XLSX).
     * The service layer will process the ZIP, extract data from PDFs, and map it to the detail report rows.
     * The processed data is then staged for user review.
     *
     * @param file       The ZIP file uploaded by the user.
     * @param carrier    The name of the carrier (e.g., "FirstNet").
     * @param uploadedBy The identifier of the user performing the upload.
     * @return A ResponseEntity containing an ApiResponse with the batch ID for the uploaded data.
     */
    @PostMapping("/upload-zip")
    public ResponseEntity<ApiResponse<String>> uploadInvoiceZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam("carrier") String carrier,
            @RequestParam("uploadedBy") String uploadedBy) {
        log.info("InvoiceUploadController.uploadInvoiceZip() >> Entry | Carrier: {}, UploadedBy: {}", carrier, uploadedBy);

        // Basic validation for file presence and type.
        if (file.isEmpty() || !Objects.equals(file.getContentType(), "application/zip")) {
            throw new IllegalArgumentException("Please upload a valid ZIP file.");
        }

        try {
            // Delegate processing to the service layer.
            String batchId = invoiceUploadService.processZipUpload(file, carrier, uploadedBy);

            // Prepare a success response.
            ApiResponse<String> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "PENDING_APPROVAL",
                    "File processed and data is ready for review. Batch ID: " + batchId
            );

            log.info("InvoiceUploadController.uploadInvoiceZip() >> Exited Successfully | Batch ID: {}", batchId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Catch any exceptions during processing and return a server error.
            log.error("Error processing ZIP file for carrier: {}", carrier, e);
            ApiResponse<String> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_STATUS, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint to upload a single detail report file (CSV or XLSX).
     * This performs pre-validation of headers and generates invoice numbers based on account numbers.
     * The processed data is then staged for user review.
     *
     * @param file       The CSV or XLSX file uploaded by the user.
     * @param carrier    The name of the carrier.
     * @param uploadedBy The identifier of the user performing the upload.
     * @return A ResponseEntity containing an ApiResponse with the batch ID.
     */
    @PostMapping("/upload-details")
    public ResponseEntity<ApiResponse<String>> uploadDetailFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("carrier") String carrier,
            @RequestParam("uploadedBy") String uploadedBy) {
        log.info("InvoiceUploadController.uploadDetailFile() >> Entry | Carrier: {}, UploadedBy: {}", carrier, uploadedBy);

        // Basic file validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ERROR_STATUS, "Uploaded file is empty."));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".csv") && !filename.toLowerCase().endsWith(".xlsx"))) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ERROR_STATUS, "Invalid file type. Please upload a CSV or XLSX file."));
        }

        try {
            // Delegate processing to the service layer.
            String batchId = invoiceUploadService.processDetailFile(file, carrier, uploadedBy);
            ApiResponse<String> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "PENDING_APPROVAL",
                    "File processed successfully and data is ready for review. Batch ID: " + batchId
            );
            log.info("InvoiceUploadController.uploadDetailFile() >> Exited Successfully | Batch ID: {}", batchId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing detail file for carrier {}: {}", carrier, e.getMessage());
            // Return specific validation errors to the user with a BAD_REQUEST status.
            ApiResponse<String> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Processing Error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Partially updates a FirstNet invoice to set a new value for total reoccurring charges.
     *
     * @param id      The ID of the invoice to be updated.
     * @param request The request body containing the new recurring charges value.
     * @return A ResponseEntity containing the updated invoice.
     */
    @PatchMapping("/{id}/recurring-charges")
    public ResponseEntity<ApiResponse<Object>> updateRecurringCharges(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecurringChargesRequest request) {

        log.info("InvoiceController.updateRecurringCharges >> Entry | Invoice ID: {} | Carrier: {}", id, request.getCarrier());

        Object updatedInvoice = invoiceUploadService.updateRecurringCharges(
                id,
                request.getNewRecurringCharges(),
                request.getCarrier()
        );

        ApiResponse<Object> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Successfully updated recurring charges.",
                updatedInvoice
        );
        log.info("InvoiceController.updateRecurringCharges >> Exit");
        return ResponseEntity.ok(response);
    }

}
