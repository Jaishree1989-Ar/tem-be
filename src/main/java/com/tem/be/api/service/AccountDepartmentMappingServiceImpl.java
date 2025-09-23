package com.tem.be.api.service;

import com.tem.be.api.dao.AccountDepartmentMappingDao;
import com.tem.be.api.dto.AccountDepartmentMappingDTO;
import com.tem.be.api.exception.DuplicateResourceException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.AccountDepartmentMapping;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Transactional
@Log4j2
public class AccountDepartmentMappingServiceImpl implements AccountDepartmentMappingService {

    private final AccountDepartmentMappingDao mappingDao;

    @Autowired
    public AccountDepartmentMappingServiceImpl(AccountDepartmentMappingDao mappingDao) {
        this.mappingDao = mappingDao;
    }

    /**
     * Retrieves all account-department mappings.
     *
     * @return A list of all mappings.
     */
    @Override
    public List<AccountDepartmentMapping> getMappingsByCarrier(String carrier) {
        log.info("Fetching account-department mappings for carrier: {}", carrier);
        List<AccountDepartmentMapping> mappings = mappingDao.findByCarrierAndIsDeletedFalseOrderByCreatedAtDesc(carrier);
        log.info("Found {} mappings for carrier: {}", mappings.size(), carrier);
        return mappings;
    }

    /**
     * Retrieves a single mapping by its ID.
     *
     * @param id The ID of the mapping.
     * @return The found mapping.
     */
    @Override
    public AccountDepartmentMapping getMappingById(Long id) {
        log.info("Fetching mapping with ID: {}", id);
        return mappingDao.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mapping not found with ID: " + id));
    }

    /**
     * Creates a new account-department mapping.
     *
     * @param mappingDTO The DTO containing mapping details.
     * @return The newly created mapping entity.
     */
    @Override
    public AccountDepartmentMapping createMapping(AccountDepartmentMappingDTO mappingDTO) {
        log.info("Creating new mapping for FAN: {}", mappingDTO.getFoundationAccountNumber());

        if (mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse(mappingDTO.getDepartmentAccountNumber())) {
            throw new DuplicateResourceException("Mapping with Account # '" + mappingDTO.getDepartmentAccountNumber() + "' already exists.");
        }

        AccountDepartmentMapping mapping = new AccountDepartmentMapping();
        mapping.setFoundationAccountNumber(mappingDTO.getFoundationAccountNumber());
        mapping.setDepartmentAccountNumber(mappingDTO.getDepartmentAccountNumber());
        mapping.setDepartment(mappingDTO.getDepartment());
        mapping.setCarrier(mappingDTO.getCarrier());
        mapping.setCreatedBy(mappingDTO.getCreatedBy());

        return mappingDao.save(mapping);
    }

    /**
     * Updates an existing mapping.
     *
     * @param id         The ID of the mapping to update.
     * @param mappingDTO The DTO with updated details.
     * @return The updated mapping entity.
     */
    @Override
    public AccountDepartmentMapping updateMapping(Long id, AccountDepartmentMappingDTO mappingDTO) {
        log.info("Updating mapping with ID: {}", id);
        AccountDepartmentMapping existingMapping = getMappingById(id);

        // Check for duplicates only if the unique account number is being changed
        if (!Objects.equals(existingMapping.getDepartmentAccountNumber(), mappingDTO.getDepartmentAccountNumber()) &&
                mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse(mappingDTO.getDepartmentAccountNumber())) {
            throw new DuplicateResourceException("Mapping with Account # '" + mappingDTO.getDepartmentAccountNumber() + "' already exists.");
        }

        existingMapping.setFoundationAccountNumber(mappingDTO.getFoundationAccountNumber());
        existingMapping.setDepartmentAccountNumber(mappingDTO.getDepartmentAccountNumber());
        existingMapping.setDepartment(mappingDTO.getDepartment());
        existingMapping.setCarrier(mappingDTO.getCarrier());

        return mappingDao.save(existingMapping);
    }

    /**
     * Deletes a mapping by its ID.
     *
     * @param id The ID of the mapping to delete.
     */
    @Override
    public void deleteMapping(Long id) {
        log.info("Deleting mapping with ID: {}", id);
        if (!mappingDao.existsById(id)) {
            throw new ResourceNotFoundException("Mapping not found with ID: " + id);
        }
        mappingDao.deleteById(id);
    }

    /**
     * Processes a CSV or XLSX file to bulk-upload mappings, applying a single carrier to all.
     *
     * @param file       The file containing mapping data (FAN, ACCOUNT #, DEPT).
     * @param uploadedBy The identifier of the user uploading the file.
     * @param carrier    The carrier to be associated with all records in the file.
     * @return A list of the mappings created or updated from the file.
     * @throws IOException if there is an error reading the file.
     */
    @Override
    public List<AccountDepartmentMapping> uploadMappings(MultipartFile file, String uploadedBy, String carrier) throws IOException {
        String originalFilename = file.getOriginalFilename();
        log.info("Starting bulk upload from file: {} by user: {} for carrier: {}", originalFilename, uploadedBy, carrier);

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty.");
        }

        List<String[]> parsedData;
        String fileSource;

        // Step 1: Parse the file into a standardized List of String arrays
        if (originalFilename.toLowerCase().endsWith(".csv")) {
            parsedData = parseCsvFile(file);
            fileSource = "CSV";
        } else if (originalFilename.toLowerCase().endsWith(".xlsx")) {
            parsedData = parseXlsxFile(file);
            fileSource = "XLSX";
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload a .csv or .xlsx file.");
        }

        // Step 2: Process the parsed data using a single, unified stream pipeline
        List<AccountDepartmentMapping> mappingsToSave = parsedData.stream()
                // ADD THIS LINE: Filter out any rows that don't have at least 3 columns
                .filter(parts -> parts.length >= 3)
                // Filter out records that are already active in the database
                .filter(parts -> !isDuplicateInDb(parts[1], fileSource))
                // Map the valid string arrays to AccountDepartmentMapping entities
                .map(parts -> createMappingFromFileData(parts, uploadedBy, file, carrier))
                .toList();

        if (mappingsToSave.isEmpty()) {
            log.warn("No new, valid mappings found to save from file: {}", originalFilename);
            return new ArrayList<>();
        }

        List<AccountDepartmentMapping> savedMappings = mappingDao.saveAll(mappingsToSave);
        log.info("Successfully uploaded and saved {} new mappings from file: {}", savedMappings.size(), originalFilename);
        return savedMappings;
    }

    /**
     * Parses a CSV file into a list of string arrays.
     */
    private List<String[]> parseCsvFile(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1) // Skip header row
                    .map(line -> line.split(","))
                    .filter(parts -> parts.length >= 3) // Ensure row is not malformed
                    .toList();
        }
    }

    /**
     * Parses an XLSX file into a list of string arrays.
     */
    private List<String[]> parseXlsxFile(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            return StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Skip header row
                    .map(this::mapRowToParts)
                    .filter(Objects::nonNull) // Filter out empty or unmappable rows
                    .toList();
        }
    }

    /**
     * A helper method to check for duplicates and log a warning.
     *
     * @param deptAcctNum The department account number to check.
     * @param fileSource  The source of the data (e.g., "CSV", "XLSX") for logging.
     * @return true if the account number already exists for an active record.
     */
    private boolean isDuplicateInDb(String deptAcctNum, String fileSource) {
        if (mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse(deptAcctNum)) {
            log.warn("Skipping duplicate Account # '{}' from {} file", deptAcctNum, fileSource);
            return true;
        }
        return false;
    }

    /**
     * Converts a POI Row object into a standard String array. Returns null for invalid rows.
     */
    private String[] mapRowToParts(Row row) {
        String deptAcctNum = getCellStringValue(row.getCell(1));
        if (deptAcctNum == null || deptAcctNum.isEmpty()) {
            return new String[0];
        }
        String[] parts = new String[3];
        parts[0] = getCellStringValue(row.getCell(0)); // FAN
        parts[1] = deptAcctNum;                          // ACCOUNT #
        parts[2] = getCellStringValue(row.getCell(2)); // DEPT
        return parts;
    }

    /**
     * Creates an AccountDepartmentMapping entity from parsed file data.
     */
    private AccountDepartmentMapping createMappingFromFileData(String[] parts, String uploadedBy, MultipartFile file, String carrier) {
        AccountDepartmentMapping mapping = new AccountDepartmentMapping();
        mapping.setFoundationAccountNumber(parts[0].trim());
        mapping.setDepartmentAccountNumber(parts[1].trim());
        mapping.setDepartment(parts[2].trim());
        mapping.setCarrier(carrier); // Apply the carrier from the request parameter
        mapping.setUploadedBy(uploadedBy);
        mapping.setFileName(file.getOriginalFilename());
        mapping.setFileType(file.getContentType());
        mapping.setFileSize(file.getSize() / 1024 + " KB");
        return mapping;
    }

    /**
     * Safely retrieves the string value from a POI Cell, handling different cell types.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Format as a whole number string to avoid scientific notation like "1.23E11"
                return String.format("%.0f", cell.getNumericCellValue());
            default:
                return "";
        }
    }

    @Override
    public Map<String, String> getDepartmentMapping(String carrier) {
        List<AccountDepartmentMapping> mappings = mappingDao.findAllByCarrier(carrier);
        return mappings.stream()
                .collect(Collectors.toMap(AccountDepartmentMapping::getDepartmentAccountNumber, AccountDepartmentMapping::getDepartment, (existing, replacement) -> existing));
    }
}
