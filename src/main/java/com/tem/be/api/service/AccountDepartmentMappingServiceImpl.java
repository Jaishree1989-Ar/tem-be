package com.tem.be.api.service;

import com.tem.be.api.dao.AccountDepartmentMappingDao;
import com.tem.be.api.dto.AccountDepartmentMappingDTO;
import com.tem.be.api.dto.MappingRow;
import com.tem.be.api.exception.DuplicateResourceException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.AccountDepartmentMapping;
import com.tem.be.api.utils.CarrierConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
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
     * Uploads and processes department-account mappings from a CSV or XLSX file for a given carrier.
     * <p>
     * The method performs the following steps:
     * <ul>
     *   <li>Validates the uploaded file name.</li>
     *   <li>Parses the file into a standardized list of {@link MappingRow} objects based on carrier type.</li>
     *   <li>Filters out duplicate mappings that already exist in the database.</li>
     *   <li>Maps the valid rows to {@link AccountDepartmentMapping} entities and saves them.</li>
     * </ul>
     *
     * @param file       the uploaded file (CSV or XLSX) containing mapping details
     * @param uploadedBy the username of the person uploading the file
     * @param carrier    the carrier type (e.g., AT&T, FirstNet, Verizon)
     * @return a list of newly saved {@link AccountDepartmentMapping} entities
     * @throws IOException              if there is an error reading the file
     * @throws IllegalArgumentException if the file name is invalid or carrier is unsupported
     */
    @Override
    public List<AccountDepartmentMapping> uploadMappings(MultipartFile file, String uploadedBy, String carrier) throws IOException {
        String originalFilename = file.getOriginalFilename();
        log.info("Starting bulk upload from file: {} by user: {} for carrier: {}", originalFilename, uploadedBy, carrier);

        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty.");
        }

        List<MappingRow> parsedRows;
        String fileSource = originalFilename.toLowerCase().endsWith(".csv") ? "CSV" : "XLSX";

        // Step 1: Use a switch to select the correct parsing logic based on the carrier.
        // The output of this step is a standardized List<MappingRow>.
        parsedRows = switch (carrier.toLowerCase()) {
            case CarrierConstants.FIRSTNET_LC, CarrierConstants.ATT_LC -> parseThreeColumnFile(file);
            case CarrierConstants.VERIZON_WIRELESS_LC -> parseVerizonFile(file);
            default ->
                    throw new IllegalArgumentException("Unsupported carrier for department mapping upload: " + carrier);
        };

        if (parsedRows.isEmpty()) {
            log.warn("No valid data rows found in file: {}", originalFilename);
            return new ArrayList<>();
        }

        // Step 2: Process the standardized list of rows. This logic is now carrier-agnostic.
        List<AccountDepartmentMapping> mappingsToSave = parsedRows.stream()
                // Filter out records that are already active in the database
                .filter(row -> !isDuplicateInDb(row.getDepartmentAccountNumber(), fileSource))
                // Map the valid MappingRow DTOs to AccountDepartmentMapping entities
                .map(row -> createMappingFromRow(row, uploadedBy, file, carrier))
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
     * Parses a 3-column file (FirstNet, AT&T) into the standardized MappingRow DTO.
     */
    private List<MappingRow> parseThreeColumnFile(MultipartFile file) throws IOException {
        List<String[]> data = readAllRows(file);
        return data.stream()
                .filter(parts -> parts.length >= 3)
                .map(parts -> new MappingRow(
                        parts[0].trim(), // FAN or Foundation Account
                        parts[1].trim(), // Account # or Billing account number
                        parts[2].trim()  // Department
                ))
                .toList();
    }

    /**
     * Parses a 2-column file (Verizon) into the standardized MappingRow DTO.
     */
    private List<MappingRow> parseVerizonFile(MultipartFile file) throws IOException {
        List<String[]> data = readAllRows(file);
        return data.stream()
                .filter(parts -> parts.length >= 2)
                .map(parts -> new MappingRow(
                        null,            // Verizon has no FAN in this file
                        parts[0].trim(), // Account number
                        parts[1].trim()  // Department
                ))
                .toList();
    }

    /**
     * Generic file reader that detects file type and returns a List of String arrays.
     */
    private List<String[]> readAllRows(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) return Collections.emptyList();

        if (filename.toLowerCase().endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().skip(1).map(line -> line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)")).toList(); // CSV-safe split
            }
        } else if (filename.toLowerCase().endsWith(".xlsx")) {
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                return StreamSupport.stream(sheet.spliterator(), false)
                        .skip(1)
                        .map(this::mapRowToStringArray)
                        .filter(arr -> arr.length > 0)
                        .toList();
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload a .csv or .xlsx file.");
        }
    }

    /**
     * A helper method to check for duplicates and log a warning.
     */
    private boolean isDuplicateInDb(String deptAcctNum, String fileSource) {
        if (deptAcctNum == null || deptAcctNum.isBlank()) {
            return true; // Skip rows with no account number
        }
        if (mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse(deptAcctNum)) {
            log.warn("Skipping duplicate Account # '{}' from {} file", deptAcctNum, fileSource);
            return true;
        }
        return false;
    }

    /**
     * Creates an AccountDepartmentMapping entity from the standardized MappingRow DTO.
     */
    private AccountDepartmentMapping createMappingFromRow(MappingRow row, String uploadedBy, MultipartFile file, String carrier) {
        AccountDepartmentMapping mapping = new AccountDepartmentMapping();
        mapping.setFoundationAccountNumber(row.getFoundationAccountNumber());
        mapping.setDepartmentAccountNumber(row.getDepartmentAccountNumber());
        mapping.setDepartment(row.getDepartment());
        mapping.setCarrier(carrier);
        mapping.setUploadedBy(uploadedBy);
        mapping.setFileName(file.getOriginalFilename());
        mapping.setFileType(file.getContentType());
        mapping.setFileSize(file.getSize() / 1024 + " KB");
        return mapping;
    }

    /**
     * Helper to convert an XLSX row to a string array.
     */
    private String[] mapRowToStringArray(Row row) {
        if (row == null) return new String[0];
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum <= 0) return new String[0];

        List<String> parts = new ArrayList<>();
        for (int i = 0; i < lastCellNum; i++) {
            parts.add(getCellStringValue(row.getCell(i)));
        }

        // Return only if the array contains non-blank data
        if (parts.stream().allMatch(String::isBlank)) return new String[0];

        return parts.toArray(new String[0]);
    }

    /**
     * Safely retrieves the string value from a POI Cell.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    @Override
    public Map<String, String> getDepartmentMapping(String carrier) {
        List<AccountDepartmentMapping> mappings = mappingDao.findAllByCarrier(carrier);
        return mappings.stream()
                .collect(Collectors.toMap(AccountDepartmentMapping::getDepartmentAccountNumber, AccountDepartmentMapping::getDepartment, (existing, replacement) -> existing));
    }
}
