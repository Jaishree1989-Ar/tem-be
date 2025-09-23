package com.tem.be.api.controller;

import com.tem.be.api.dto.AccountDepartmentMappingDTO;
import com.tem.be.api.model.AccountDepartmentMapping;
import com.tem.be.api.service.AccountDepartmentMappingService;
import com.tem.be.api.utils.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Log4j2
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/mappings")
public class AccountDepartmentMappingController {

    private final AccountDepartmentMappingService mappingService;
    private static final String UPLOAD_MAPPINGS_EXIT_LOG = "AccountDepartmentMappingController.uploadMappings >> Exit";

    @Autowired
    public AccountDepartmentMappingController(AccountDepartmentMappingService mappingService) {
        this.mappingService = mappingService;
    }

    /**
     * Retrieves all account-department mappings.
     *
     * @return A ResponseEntity containing a list of all mappings.
     */
    @GetMapping("/carrier/{carrier}")
    public ResponseEntity<ApiResponse<List<AccountDepartmentMapping>>> getMappingsByCarrier(@PathVariable String carrier) {
        log.info("AccountDepartmentMappingController.getMappingsByCarrier >> Entry for carrier: {}", carrier);
        try {
            List<AccountDepartmentMapping> mappings = mappingService.getMappingsByCarrier(carrier);
            ApiResponse<List<AccountDepartmentMapping>> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", mappings);
            log.info("AccountDepartmentMappingController.getMappingsByCarrier >> Exit with {} mappings", mappings.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getMappingsByCarrier for carrier: {}", carrier, e);
            ApiResponse<List<AccountDepartmentMapping>> errorResponse = new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error fetching mappings: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves a specific mapping by its ID.
     *
     * @param id The ID of the mapping.
     * @return A ResponseEntity containing the requested mapping.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDepartmentMapping>> getMappingById(@PathVariable Long id) {
        log.info("AccountDepartmentMappingController.getMappingById >> Entry");
        AccountDepartmentMapping mapping = mappingService.getMappingById(id);
        ApiResponse<AccountDepartmentMapping> response = new ApiResponse<>(HttpStatus.OK.value(), "Success", mapping);
        log.info("AccountDepartmentMappingController.getMappingById >> Exit");
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new account-department mapping.
     *
     * @param mappingDTO The DTO with the details for the new mapping.
     * @return A ResponseEntity containing the created mapping.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountDepartmentMapping>> createMapping(@RequestBody AccountDepartmentMappingDTO mappingDTO) {
        log.info("AccountDepartmentMappingController.createMapping >> Entry");
        AccountDepartmentMapping newMapping = mappingService.createMapping(mappingDTO);
        ApiResponse<AccountDepartmentMapping> response = new ApiResponse<>(HttpStatus.CREATED.value(), "Mapping created successfully", newMapping);
        log.info("AccountDepartmentMappingController.createMapping >> Exit");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Updates an existing mapping.
     *
     * @param id         The ID of the mapping to update.
     * @param mappingDTO The DTO with the updated details.
     * @return A ResponseEntity containing the updated mapping.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDepartmentMapping>> updateMapping(@PathVariable Long id, @RequestBody AccountDepartmentMappingDTO mappingDTO) {
        log.info("AccountDepartmentMappingController.updateMapping >> Entry");
        AccountDepartmentMapping updatedMapping = mappingService.updateMapping(id, mappingDTO);
        ApiResponse<AccountDepartmentMapping> response = new ApiResponse<>(HttpStatus.OK.value(), "Mapping updated successfully", updatedMapping);
        log.info("AccountDepartmentMappingController.updateMapping >> Exit");
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a mapping by its ID.
     *
     * @param id The ID of the mapping to delete.
     * @return A ResponseEntity with a success message.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMapping(@PathVariable Long id) {
        log.info("AccountDepartmentMappingController.deleteMapping >> Entry");
        mappingService.deleteMapping(id);
        ApiResponse<String> response = new ApiResponse<>(HttpStatus.OK.value(), HttpStatus.OK.toString(), "Mapping with ID " + id + " was deleted.");
        log.info("AccountDepartmentMappingController.deleteMapping >> Exit");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to upload a CSV file for bulk creation of mappings.
     * The CSV file should have columns: FAN, ACCOUNT #, DEPT
     *
     * @param file       The multipart file (CSV).
     * @param uploadedBy Identifier for the user performing the upload.
     * @return A ResponseEntity with the result of the upload.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<AccountDepartmentMapping>>> uploadMappings(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam("carrier") String carrier) {
        log.info("AccountDepartmentMappingController.uploadMappings >> Entry");
        try {
            List<AccountDepartmentMapping> createdMappings = mappingService.uploadMappings(file, uploadedBy, carrier);

            if (!createdMappings.isEmpty()) {
                String message = "Successfully uploaded and processed " + createdMappings.size() + " new mappings.";
                ApiResponse<List<AccountDepartmentMapping>> response = new ApiResponse<>(HttpStatus.CREATED.value(), message, createdMappings);
                log.info("Successfully created {} new mappings from file: {}", createdMappings.size(), file.getOriginalFilename());
                log.info(UPLOAD_MAPPINGS_EXIT_LOG);
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            } else {
                String message = "File processed successfully, but no new mappings were added. All records may have been duplicates or invalid.";
                ApiResponse<List<AccountDepartmentMapping>> response = new ApiResponse<>(HttpStatus.OK.value(), message, createdMappings);
                log.warn("File {} processed, but contained no new valid mappings to save.", file.getOriginalFilename());
                log.info(UPLOAD_MAPPINGS_EXIT_LOG);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (IOException e) {
            log.error("Failed to read or process the uploaded file: {}", e.getMessage(), e);
            ApiResponse<List<AccountDepartmentMapping>> response = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to process file due to a server error.", null);
            log.info(UPLOAD_MAPPINGS_EXIT_LOG);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            log.error("Invalid file type or content provided: {}", e.getMessage());
            ApiResponse<List<AccountDepartmentMapping>> response = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
            log.info(UPLOAD_MAPPINGS_EXIT_LOG);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
