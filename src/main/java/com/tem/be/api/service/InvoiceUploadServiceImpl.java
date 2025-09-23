package com.tem.be.api.service;

import com.tem.be.api.dao.*;
import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InvoiceFilterDto;
import com.tem.be.api.enums.FileType;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InvoiceProcessingException;
import com.tem.be.api.model.*;
import com.tem.be.api.service.processors.InvoiceProcessor;
import com.tem.be.api.service.processors.InvoiceProcessorFactory;
import com.tem.be.api.utils.FileParsingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for invoice upload related operations.
 */
@Service
@Log4j2
public class InvoiceUploadServiceImpl implements InvoiceUploadService {

    private final FileParsingUtil fileParsingUtil;
    private final FirstNetInvoiceDao firstnetDao;
    private final ATTInvoiceDao attDao;

    private final InvoiceTransactionalService invoiceTransactionalService;
    private final InvoiceProcessorFactory invoiceProcessorFactory;

    private final InvoiceHistoryService invoiceHistoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String NORMALIZED_KEY_DEPARTMENT = "department";
    private static final String NORMALIZED_KEY_BILLING_ACCOUNT_NAME = "billingAccountName";
    private static final String NORMALIZED_KEY_UDL2 = "udl2";

    private static final String CSV_EXTENSION = ".csv";
    private static final String EXCEL_EXTENSION = ".xlsx";

    @Autowired
    public InvoiceUploadServiceImpl(FileParsingUtil fileParsingUtil, FirstNetInvoiceDao firstnetDao,
                                    ATTInvoiceDao attDao,
                                    InvoiceHistoryService invoiceHistoryService,
                                    InvoiceTransactionalService invoiceTransactionalService,
                                    InvoiceProcessorFactory invoiceProcessorFactory) {
        this.fileParsingUtil = fileParsingUtil;
        this.firstnetDao = firstnetDao;
        this.attDao = attDao;
        this.invoiceHistoryService = invoiceHistoryService;
        this.invoiceTransactionalService = invoiceTransactionalService;
        this.invoiceProcessorFactory = invoiceProcessorFactory;
    }

    /**
     * Searches for invoices based on the specified carrier and filter criteria.
     * This method dynamically builds a JPA Specification to query the correct
     * invoice type (e.g., FirstNet, AT&T) and applies the provided filters.
     *
     * @param carrier  The name of the carrier (e.g., "firstnet", "att"). Must not be null or blank.
     * @param filter   An {@link InvoiceFilterDto} containing the criteria for filtering the invoices.
     * @param pageable A {@link Pageable} object for pagination and sorting.
     * @return A {@link Page} of {@link Invoiceable} objects that match the filter criteria.
     * @throws IllegalArgumentException      if the carrier is null, blank, or unsupported.
     * @throws UnsupportedOperationException if filtering for the specified carrier is not yet implemented.
     */
    @Override
    public Page<Invoiceable> searchInvoices(String carrier, InvoiceFilterDto filter, Pageable pageable) {
        log.info("Attempting to search invoices for carrier: {} with filters: {}", carrier, filter);

        if (carrier == null || carrier.isBlank()) {
            log.error("Carrier parameter was not provided.");
            throw new IllegalArgumentException("Carrier parameter must be provided.");
        }

        return switch (carrier.toLowerCase()) {
            case "firstnet" -> {
                Specification<FirstNetInvoice> spec = InvoiceSpecifications.findByCriteria(filter);
                log.debug("Executing FirstNet invoice search with spec: {} and pageable: {}", spec, pageable);
                yield firstnetDao.findAll(spec, pageable).map(Function.identity());
            }
            case "at&t mobility" -> {
                Specification<ATTInvoice> spec = InvoiceSpecifications.findByCriteria(filter);
                log.debug("Executing ATT invoice search with spec: {} and pageable: {}", spec, pageable);
                yield attDao.findAll(spec, pageable).map(Function.identity());
            }
            default -> {
                log.error("Unsupported carrier specified: {}", carrier);
                throw new IllegalArgumentException("Unsupported carrier for filtering: " + carrier);
            }
        };
    }

    @Override
    public DepartmentDistinctDTO getDistinctDepartments(String carrier) {
        List<String> departments;

        // Use a simple if/else if block to select the correct DAO
        if ("firstnet".equalsIgnoreCase(carrier)) {
            departments = firstnetDao.findDistinctDepartments();
        } else if ("at&t mobility".equalsIgnoreCase(carrier)) {
            departments = attDao.findDistinctDepartments();
        } else {
            // Handle the case where the carrier is not supported
            throw new IllegalArgumentException("Unsupported carrier: " + carrier);
        }

        return new DepartmentDistinctDTO(departments);
    }

    /**
     * Processes an uploaded ZIP file containing PDFs and a detail report (CSV/XLSX).
     * The process involves:
     * 1. Creating a parent {@link InvoiceHistory} record to represent the batch.
     * 2. Unpacking the ZIP file into memory.
     * 3. Parsing all PDFs to create a mapping from account numbers to invoice numbers.
     * 4. Parsing the detail report (CSV/XLSX).
     * 5. Enriching the detail report data with the invoice numbers from the PDF mapping.
     * 6. Saving the enriched data as {@link TempFirstNetInvoice} records, linked to the parent history.
     *
     * @param zipFile    The multipart file representing the uploaded ZIP archive.
     * @param carrier    The name of the carrier.
     * @param uploadedBy The identifier of the user who uploaded the file.
     * @return A unique batch ID string for tracking this upload.
     * @throws IOException if there is an error reading the file streams.
     */
    @Override
    public String processZipUpload(MultipartFile zipFile, String carrier, String uploadedBy) throws IOException {
        log.info("InvoiceUploadServiceImpl.processZipUpload() >> Entry | Filename: {}, carrier: {}", zipFile.getOriginalFilename(), carrier);

        String batchId = UUID.randomUUID().toString();

        // 1. Create the initial history record. This is committed immediately.
        InvoiceHistory history = new InvoiceHistory();
        history.setBatchId(batchId);
        history.setCarrier(carrier);
        history.setName(zipFile.getOriginalFilename());
        history.setFileType(zipFile.getContentType());
        history.setFileSize(String.valueOf(zipFile.getSize()));
        history.setUploadedBy(uploadedBy);
        history.setStatus(InvoiceStatus.PENDING_APPROVAL); // Start with a pending status
        InvoiceHistory savedHistory = invoiceHistoryService.createInvoiceHistory(history);
        log.debug("Saved initial parent InvoiceHistory with ID: {} and Batch ID: {}", savedHistory.getInvoiceId(), batchId);


        try {
            // 2. Perform all fallible processing within this try block.
            // Unpack the ZIP file...
            Map<String, byte[]> zipContents = unpackZip(zipFile);

            // STAGE 1: Process PDFs...
            Map<String, String> accountNumberToInvoiceNumberMap = processPdfsInZip(zipContents);

            // Fail early if no mappings could be created...
            if (accountNumberToInvoiceNumberMap.isEmpty()) {
                throw new IllegalArgumentException("No valid PDFs with Account and Invoice numbers found in the ZIP.");
            }

            // STAGE 2: Process CSV/XLSX...
            List<TempFirstNetInvoice> tempInvoices = processDetailFilesInZip(zipContents, savedHistory, accountNumberToInvoiceNumberMap, carrier);

            if (tempInvoices.isEmpty()) {
                throw new IllegalArgumentException("No valid CSV or XLSX data rows found in the ZIP file.");
            }

            // 3. Save all temporary invoices in a single transaction.
            invoiceTransactionalService.saveTempInvoices(tempInvoices);
            log.info("Saved {} temporary invoice records for Batch ID: {}", tempInvoices.size(), batchId);

            log.info("InvoiceUploadServiceImpl.processZipUpload() >> Exited Successfully | Batch ID: {}", batchId);
            return batchId;

        } catch (Exception e) {
            // 4. If any exception occurs, mark the history record as FAILED.
            invoiceHistoryService.markAsFailed(batchId, e.getMessage());
            log.error("Processing failed for Batch ID: {}. Marked as FAILED.", batchId, e);
            // Re-throw the exception so the controller can return a proper HTTP error.
            throw e;
        }
    }

    /**
     * Unpacks an uploaded ZIP file into an in-memory map for efficient processing.
     * The map keys are the filenames within the ZIP, and the values are their byte array contents.
     *
     * @param zipFile The multipart file representing the uploaded ZIP archive.
     * @return A map containing the contents of the ZIP file.
     * @throws IOException if there is an error reading the file stream.
     */
    private Map<String, byte[]> unpackZip(MultipartFile zipFile) throws IOException {
        Map<String, byte[]> zipContents = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    zipContents.put(zipEntry.getName(), zis.readAllBytes());
                }
            }
        } catch (java.util.zip.ZipException e) {
            log.error("Failed to process ZIP file, it may be corrupted. Error: {}", e.getMessage());
            // Re-throw with a user-friendly message
            throw new IOException("The file appears to be a corrupted or invalid ZIP file.", e);
        }
        return zipContents;
    }

    /**
     * Processes all PDF files from the unpacked ZIP contents to build a mapping
     * from account numbers to invoice numbers.
     *
     * @param zipContents A map of filenames to their byte contents from the ZIP file.
     * @return A map where the key is the account number and the value is the invoice number.
     * @throws IOException if there is an error reading the byte array streams.
     */
    private Map<String, String> processPdfsInZip(Map<String, byte[]> zipContents) throws IOException {
        Map<String, String> accountNumberToInvoiceNumberMap = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : zipContents.entrySet()) {
            if (entry.getKey().toLowerCase().endsWith(".pdf")) {
                try (InputStream is = new ByteArrayInputStream(entry.getValue())) {
                    Map<String, String> pdfData = fileParsingUtil.extractInvoiceAndAccountNumbers(is);
                    String accountNumber = pdfData.get("accountNumber");
                    String invoiceNumber = pdfData.get("invoiceNumber");
                    if (accountNumber != null && !accountNumber.isBlank() && invoiceNumber != null && !invoiceNumber.isBlank()) {
                        accountNumberToInvoiceNumberMap.put(accountNumber, invoiceNumber);
                        log.info("Mapped Account Number '{}' to Invoice '{}' from PDF '{}'", accountNumber, invoiceNumber, entry.getKey());
                    } else {
                        log.warn("Could not find a valid Account Number or Invoice Number in PDF: {}", entry.getKey());
                    }
                }
            }
        }
        return accountNumberToInvoiceNumberMap;
    }

    /**
     * Processes all detail report files (CSV/XLSX) from the unpacked ZIP contents.
     * It parses the files, enriches the data with invoice numbers from the PDF mapping,
     * and converts them into a list of TempFirstNetInvoice entities.
     *
     * @param zipContents                     A map of filenames to their byte contents.
     * @param savedHistory                    The parent InvoiceHistory record for this batch.
     * @param accountNumberToInvoiceNumberMap The mapping generated from processing PDFs.
     * @param carrier                         The name of the carrier.
     * @return A list of enriched TempFirstNetInvoice entities ready to be saved.
     * @throws IOException if there is an error reading the file streams.
     */
    private List<TempFirstNetInvoice> processDetailFilesInZip(Map<String, byte[]> zipContents, InvoiceHistory savedHistory, Map<String, String> accountNumberToInvoiceNumberMap, String carrier) throws IOException {
        List<TempFirstNetInvoice> tempInvoices = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : zipContents.entrySet()) {
            String filename = entry.getKey().toLowerCase();
            if (filename.endsWith(CSV_EXTENSION) || filename.endsWith(EXCEL_EXTENSION)) {
                try (InputStream is = new ByteArrayInputStream(entry.getValue())) {
                    // Parse the raw data from the file.
                    List<Map<String, String>> data = parseFile(filename, is, carrier);
                    // Convert raw data into temporary entity objects.
                    List<TempFirstNetInvoice> entities = convertToTempEntities(data, savedHistory, entry.getKey());

                    // Enrich each entity with the invoice number from the PDF-derived map.
                    for (TempFirstNetInvoice invoice : entities) {
                        String fullAccountNumber = extractFullAccountNumber(invoice.getAccountAndDescriptions());
                        if (fullAccountNumber != null && accountNumberToInvoiceNumberMap.containsKey(fullAccountNumber)) {
                            invoice.setInvoiceNumber(accountNumberToInvoiceNumberMap.get(fullAccountNumber));
                        } else {
                            log.warn("No invoice number mapping found for Account in description: '{}'", invoice.getAccountAndDescriptions());
                        }
                    }
                    tempInvoices.addAll(entities);
                }
            }
        }
        return tempInvoices;
    }

    @Override
    public String processDetailFile(MultipartFile detailFile, String carrier, String uploadedBy) throws InvoiceProcessingException {
        log.info("Processing detail file '{}' for carrier '{}'", detailFile.getOriginalFilename(), carrier);
        String batchId = UUID.randomUUID().toString();
        String filename = detailFile.getOriginalFilename();

        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null.");
        }

        // 1. Create initial history record
        InvoiceHistory history = createInitialHistory(batchId, carrier, uploadedBy, detailFile);

        try {
            // 2. Get the correct processor strategy for the given carrier
            InvoiceProcessor processor = invoiceProcessorFactory.getProcessor(carrier);
            log.debug("Using processor: {}", processor.getClass().getSimpleName());

            // 3. Parse the file into a generic list of maps
            List<Map<String, String>> data = parseFile(filename, detailFile, carrier);
            if (data.isEmpty()) {
                throw new IllegalArgumentException("The file is empty or contains no data rows.");
            }

            // 4. Delegate conversion and enrichment to the selected processor
            List<? extends TempInvoiceBase> tempInvoices = processor.convertAndEnrichData(data, history, filename);

            // 5. Delegate saving to the processor (it knows the correct repository)
            processor.saveTempInvoices(tempInvoices);
            log.info("Successfully saved {} temporary invoice records for Batch ID: {}", tempInvoices.size(), batchId);

            return batchId;
        } catch (Exception e) {
            // 6. Mark history as FAILED on any exception
            String failureReason = e.getCause() instanceof ConstraintViolationException ?
                    "The file contains duplicate invoice entries." : e.getMessage();
            invoiceHistoryService.markAsFailed(batchId, failureReason);
            log.error("Processing failed for Batch ID: {}. Reason: {}", batchId, failureReason, e);
            throw new InvoiceProcessingException("Failed to process file for batch " + batchId, e);
        }
    }

    private InvoiceHistory createInitialHistory(String batchId, String carrier, String uploadedBy, MultipartFile file) {
        InvoiceHistory history = new InvoiceHistory();
        history.setBatchId(batchId);
        history.setCarrier(carrier);
        history.setName(file.getOriginalFilename());
        history.setFileType(file.getContentType());
        history.setFileSize(String.valueOf(file.getSize()));
        history.setUploadedBy(uploadedBy);
        history.setStatus(InvoiceStatus.PENDING_APPROVAL);
        return invoiceHistoryService.createInvoiceHistory(history);
    }

    private List<Map<String, String>> parseFile(String filename, MultipartFile file, String carrier) throws IOException {
        if (filename.toLowerCase().endsWith(CSV_EXTENSION)) {
            return fileParsingUtil.readCsv(file.getInputStream(), carrier, FileType.INVOICE);
        } else if (filename.toLowerCase().endsWith(EXCEL_EXTENSION)) {
            return fileParsingUtil.readXlsx(file.getInputStream(), carrier, FileType.INVOICE);
        }
        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    /**
     * Private helper to delegate parsing based on file extension.
     */
    private List<Map<String, String>> parseFile(String filename, InputStream inputStream, String carrier) throws IOException {
        if (filename.endsWith(CSV_EXTENSION)) {
            return fileParsingUtil.readCsv(inputStream, carrier, FileType.INVOICE);
        } else if (filename.endsWith(EXCEL_EXTENSION)) {
            return fileParsingUtil.readXlsx(inputStream, carrier, FileType.INVOICE);
        }
        throw new IllegalArgumentException("Unsupported file type: " + filename.substring(filename.lastIndexOf(".")));
    }

    /**
     * Helper method to convert a list of raw data maps into a list of TempFirstNetInvoice entities.
     * It associates each new entity with its parent InvoiceHistory record.
     */
    private List<TempFirstNetInvoice> convertToTempEntities(List<Map<String, String>> dataList, InvoiceHistory history, String sourceFilename) {
        List<TempFirstNetInvoice> invoices = new ArrayList<>();
        for (Map<String, String> data : dataList) {
            if (data == null || data.isEmpty()) continue;

            Map<String, String> cleaned = normalizeKeys(data); // Assumes normalizeKeys exists
            applyCustomTransformationsOnCleanedMap(cleaned);
            TempFirstNetInvoice tempInvoice = objectMapper.convertValue(cleaned, TempFirstNetInvoice.class); // Assumes objectMapper exists

            // Associate the child with its parent batch history.
            tempInvoice.setInvoiceHistory(history);

            tempInvoice.setSourceFilename(sourceFilename);
            tempInvoice.setStatus(InvoiceStatus.PENDING_APPROVAL);
            invoices.add(tempInvoice);
        }
        return invoices;
    }

    private void applyCustomTransformationsOnCleanedMap(Map<String, String> cleanedRowData) {
        // Department/Division Logic
        String originalDepartmentValue = cleanedRowData.get(NORMALIZED_KEY_DEPARTMENT);
        cleanedRowData.put("division", originalDepartmentValue); // This key matches the Java field

        String newDepartmentValue = cleanedRowData.get(NORMALIZED_KEY_BILLING_ACCOUNT_NAME);
        cleanedRowData.put(NORMALIZED_KEY_DEPARTMENT, newDepartmentValue);

        // UDL 2 to VIS Code Logic
        if (cleanedRowData.containsKey(NORMALIZED_KEY_UDL2)) {
            String udl2Value = cleanedRowData.remove(NORMALIZED_KEY_UDL2);

            cleanedRowData.put("visCode", udl2Value);
        }
    }

    /**
     * Extracts the leading numeric account number from a descriptive string.
     * Example: "287298936374 (CITY OF SAN JOSE PUBLIC WORKS)" -> "287298936374"
     *
     * @param description The full string from the "Account and descriptions" column.
     * @return The extracted account number string, or null if not found.
     */
    private String extractFullAccountNumber(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        // This regex finds a sequence of digits at the beginning of the string.
        Pattern pattern = Pattern.compile("^(\\d+)");
        Matcher matcher = pattern.matcher(description.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Map<String, String> normalizeKeys(Map<String, String> row) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String key = entry.getKey().trim();
            String camelKey = toCamelCase(key);
            normalized.put(camelKey, entry.getValue().trim());
        }
        return normalized;
    }

    private String toCamelCase(String input) {
        String[] parts = input.toLowerCase().split(" ");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(parts[i].substring(0, 1).toUpperCase())
                    .append(parts[i].substring(1));
        }
        return camelCase.toString();
    }

    @Override
    @Transactional
    public Object updateRecurringCharges(Long invoiceId, BigDecimal newRecurringCharges, String carrier) {
        log.info("Attempting to update recurring charges | Invoice ID: {} | Carrier: {} | New Value: {}",
                invoiceId, carrier, newRecurringCharges);

        return switch (carrier.toUpperCase()) {
            case "FIRSTNET" -> {
                FirstNetInvoice invoice = firstnetDao.findById(invoiceId)
                        .orElseThrow(() -> new EntityNotFoundException("FirstNetInvoice not found with ID: " + invoiceId));
                invoice.setTotalReoccurringCharges(newRecurringCharges);
                yield firstnetDao.save(invoice);
            }
            case "AT&T MOBILITY" -> {
                ATTInvoice invoice = attDao.findById(invoiceId)
                        .orElseThrow(() -> new EntityNotFoundException("AttInvoice not found with ID: " + invoiceId));
                invoice.setTotalReoccurringCharges(newRecurringCharges);
                yield attDao.save(invoice);
            }
            default -> throw new IllegalArgumentException("Unsupported or unknown carrier: " + carrier);
        };
    }

}
