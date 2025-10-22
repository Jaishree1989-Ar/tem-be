package com.tem.be.api.controller;

import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InventoryFilterDto;
import com.tem.be.api.model.Inventoryable;
import com.tem.be.api.service.InventoryUploadService;
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

/**
 * REST controller for managing inventory upload operations.
 */
@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/inventory")
public class InventoryUploadController {

    private final InventoryUploadService inventoryUploadService;

    @Autowired
    public InventoryUploadController(InventoryUploadService inventoryUploadService) {
        this.inventoryUploadService = inventoryUploadService;
    }

    /**
     * Handles the upload of an inventory file (CSV or XLSX).
     * This endpoint initiates the process of parsing the file, validating its structure against inventory-specific rules,
     * and staging the data for user review and approval.
     *
     * @param file       The inventory file to be uploaded, sent as multipart/form-data.
     * @param uploadedBy The username or identifier of the person uploading the file.
     * @return A {@link ResponseEntity} containing an {@link ApiResponse}.
     * On success, the response body includes a batch ID for the uploaded file,
     * and the status is PENDING_APPROVAL.
     * On failure (e.g., invalid file type, empty file, parsing error),
     * a 400 Bad Request response is returned with a descriptive error message.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadInventoryFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("carrier") String carrier,
            @RequestParam("uploadedBy") String uploadedBy) {
        final String methodName = "InventoryUploadController.uploadInventoryFile";
        log.info("{} >> Entry | Carrier: {}, UploadedBy: {}, Filename: {}", methodName, carrier, uploadedBy, file.getOriginalFilename());

        if (file.isEmpty()) {
            log.warn("{} >> Validation failed: Uploaded file is empty.", methodName);
            return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Error", "Uploaded file is empty."));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".csv") && !filename.toLowerCase().endsWith(".xlsx"))) {
            log.warn("{} >> Validation failed: Invalid file type for filename '{}'.", methodName, filename);
            return ResponseEntity.badRequest().body(new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Error", "Invalid file type. Please upload a CSV or XLSX file."));
        }

        try {
            String batchId = inventoryUploadService.processInventoryFile(file, carrier, uploadedBy);
            ApiResponse<String> response = new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "PENDING_APPROVAL",
                    "File processed successfully and data is ready for review. Batch ID: " + batchId
            );
            log.info("{} >> Exited Successfully | Batch ID: {}", methodName, batchId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("{} >> Error processing inventory file for carrier {}: {}", methodName, carrier, e.getMessage(), e);
            ApiResponse<String> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Processing Error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Retrieves a list of distinct department names for a specific carrier from the inventory data.
     * The carrier name is passed as a path variable.
     *
     * @param carrier The name of the carrier (e.g., "firstnet", "att mobility").
     * @return A ResponseEntity containing an ApiResponse with a list of distinct department names.
     */
    @GetMapping(value = "/distinct-departments/{carrier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<DepartmentDistinctDTO>> getDistinctDepartmentsByCarrier(@PathVariable String carrier) {
        log.info("InventoryHistoryController.getDistinctDepartmentsByCarrier({}) >> Entered", carrier);

        DepartmentDistinctDTO result = inventoryUploadService.getDistinctDepartments(carrier);
        ApiResponse<DepartmentDistinctDTO> response = new ApiResponse<>(HttpStatus.OK.value(), RestConstants.SUCCESS_STRING, result);

        log.info("InventoryHistoryController.getDistinctDepartmentsByCarrier({}) >> Exited Successfully", carrier);
        return ResponseEntity.ok(response);
    }

    /**
     * Searches for inventories with dynamic filter criteria, pagination, and sorting.
     * The structure of the returned objects depends on the specified 'carrier'.
     *
     * @param carrier  The carrier to search for (e.g., "FirstNet"). Required.
     * @param filter   A DTO containing all optional filter parameters.
     * @param pageable Standard pagination and sorting parameters.
     * @return A paginated list of carrier-specific inventories matching the criteria.
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Page<Inventoryable>>> searchInventories(
            @RequestParam String carrier,
            InventoryFilterDto filter,
            Pageable pageable) {

        log.info("InventoryUploadController.searchInventories() >> Entered for carrier: {} with filter: {}", carrier, filter);

        Page<Inventoryable> results = inventoryUploadService.searchInventories(carrier, filter, pageable);

        ApiResponse<Page<Inventoryable>> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", results);

        log.info("InventoryUploadController.searchInventories() >> Exited");
        return ResponseEntity.ok(response);
    }

    /**
     * Handles IllegalArgumentException specifically for this controller.
     * Returns a 400 Bad Request response, which is appropriate for invalid client input.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad Request during inventory search: {}", ex.getMessage());
        ApiResponse<String> response = new ApiResponse<>(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request", // Or keep "Processing Error" if you prefer
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}