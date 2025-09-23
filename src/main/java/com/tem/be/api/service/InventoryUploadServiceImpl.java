package com.tem.be.api.service;

import com.tem.be.api.dao.inventory.ATTInventoryDao;
import com.tem.be.api.dao.inventory.FirstNetInventoryDao;
import com.tem.be.api.dto.DepartmentDistinctDTO;
import com.tem.be.api.dto.InventoryFilterDto;
import com.tem.be.api.enums.FileType;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InventoryProcessingException;
import com.tem.be.api.model.*;
import com.tem.be.api.service.processors.InventoryProcessor;
import com.tem.be.api.service.processors.InventoryProcessorFactory;
import com.tem.be.api.utils.FileParsingUtil;
import lombok.extern.log4j.Log4j2;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Log4j2
public class InventoryUploadServiceImpl implements InventoryUploadService {

    private final InventoryHistoryService inventoryHistoryService;
    private final FileParsingUtil fileParsingUtil;
    private final FirstNetInventoryDao firstnetDao;
    private final ATTInventoryDao attDao;
    private final AccountDepartmentMappingService departmentMappingService;
    private final InventoryProcessorFactory processorFactory;

    @Autowired
    public InventoryUploadServiceImpl(InventoryHistoryService inventoryHistoryService,
                                      FileParsingUtil fileParsingUtil, FirstNetInventoryDao firstnetDao,
                                      ATTInventoryDao attDao,
                                      AccountDepartmentMappingService departmentMappingService,
                                      InventoryProcessorFactory processorFactory) {
        this.inventoryHistoryService = inventoryHistoryService;
        this.fileParsingUtil = fileParsingUtil;
        this.firstnetDao = firstnetDao;
        this.attDao = attDao;
        this.departmentMappingService = departmentMappingService;
        this.processorFactory = processorFactory;
    }

    @Override
    public String processInventoryFile(MultipartFile inventoryFile, String carrier, String uploadedBy) throws InventoryProcessingException {
        log.info("Processing inventory file: {}, Carrier: {}", inventoryFile.getOriginalFilename(), carrier);

        String batchId = UUID.randomUUID().toString();
        String filename = inventoryFile.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("Filename cannot be null.");

        InventoryHistory history = createHistoryRecord(batchId, carrier, filename, uploadedBy, inventoryFile);

        try {
            List<Map<String, String>> data = parseFile(filename, inventoryFile.getInputStream(), carrier);
            if (data.isEmpty()) {
                throw new IllegalArgumentException("The provided file is empty or contains no data rows.");
            }

            // Get the correct processor from the factory
            InventoryProcessor<? extends TempInventoryBase> processor = processorFactory.getProcessor(carrier);

            // Delegate all carrier-specific logic to the processor
            processAndSaveWithProcessor(processor, data, history, carrier, filename);

            return batchId;
        } catch (Exception e) {
            String failureReason = (e.getCause() instanceof ConstraintViolationException)
                    ? "The uploaded file contains duplicate inventory entries."
                    : e.getMessage();
            inventoryHistoryService.markAsFailed(batchId, failureReason);
            log.error("Processing failed for Batch ID: {}. Marked as FAILED.", batchId, e);
            throw new InventoryProcessingException("Failed to process inventory file for batch " + batchId, e);
        }
    }

    // Helper method to handle generics safely
    private <T extends TempInventoryBase> void processAndSaveWithProcessor(
            InventoryProcessor<T> processor, List<Map<String, String>> data, InventoryHistory history, String carrier, String filename) {

        Map<String, String> departmentMapping = departmentMappingService.getDepartmentMapping(carrier);
        List<T> tempInventories = processor.convertAndMapData(data, history, departmentMapping, filename);
        processor.saveTempInventory(tempInventories);
        log.info("Successfully processed and saved {} records for Batch ID: {}", tempInventories.size(), history.getBatchId());
    }

    private InventoryHistory createHistoryRecord(String batchId, String carrier, String filename, String uploadedBy, MultipartFile file) {
        InventoryHistory history = new InventoryHistory();
        history.setBatchId(batchId);
        history.setCarrier(carrier);
        history.setName(filename);
        history.setFileType(file.getContentType());
        history.setFileSize(String.valueOf(file.getSize()));
        history.setUploadedBy(uploadedBy);
        history.setStatus(InvoiceStatus.PENDING_APPROVAL);
        return inventoryHistoryService.createInventoryHistory(history);
    }

    private List<Map<String, String>> parseFile(String filename, InputStream inputStream, String carrier) throws IOException {
        if (filename.toLowerCase().endsWith(".csv")) {
            return fileParsingUtil.readCsv(inputStream, carrier, FileType.INVENTORY);
        } else if (filename.toLowerCase().endsWith(".xlsx")) {
            return fileParsingUtil.readXlsx(inputStream, carrier, FileType.INVENTORY);
        }
        throw new IllegalArgumentException("Unsupported file type: " + filename.substring(filename.lastIndexOf(".")));
    }

    /**
     * Retrieves distinct department names based on the specified carrier.
     * It queries the appropriate final inventory table.
     *
     * @param carrier The carrier name (e.g., "firstnet", "at&t mobility").
     * @return A DepartmentDistinctDTO containing the list of departments.
     * @throws IllegalArgumentException if the carrier is not supported.
     */
    @Override
    public DepartmentDistinctDTO getDistinctDepartments(String carrier) {
        log.info("Fetching distinct inventory departments for carrier: {}", carrier);
        List<String> departments;

        // Use if/else if block to select the correct DAO based on the carrier name
        if ("firstnet".equalsIgnoreCase(carrier)) {
            departments = firstnetDao.findDistinctDepartments();
        } else if ("at&t mobility".equalsIgnoreCase(carrier)) {
            departments = attDao.findDistinctDepartments();
        } else {
            log.warn("Attempted to fetch departments for an unsupported carrier: {}", carrier);
            throw new IllegalArgumentException("Unsupported carrier for inventory departments: " + carrier);
        }

        log.info("Found {} distinct departments for carrier: {}", departments.size(), carrier);
        return new DepartmentDistinctDTO(departments);
    }

    /**
     * Searches for inventories based on the specified carrier and filter criteria.
     * This method dynamically builds a JPA Specification to query the correct
     * inventory type (e.g., FirstNet, AT&T) and applies the provided filters.
     *
     * @param carrier  The name of the carrier (e.g., "firstnet", "at&t mobility"). Must not be null or blank.
     * @param filter   An {@link InventoryFilterDto} containing the criteria for filtering the inventories.
     * @param pageable A {@link Pageable} object for pagination and sorting.
     * @return A {@link Page} of {@link Inventoryable} objects that match the filter criteria.
     * @throws IllegalArgumentException      if the carrier is null, blank, or unsupported.
     * @throws UnsupportedOperationException if filtering for the specified carrier is not yet implemented.
     */
    @Override
    public Page<Inventoryable> searchInventories(String carrier, InventoryFilterDto filter, Pageable pageable) {
        log.info("Attempting to search inventories for carrier: {} with filters: {}", carrier, filter);

        if (carrier == null || carrier.isBlank()) {
            log.error("Carrier parameter was not provided.");
            throw new IllegalArgumentException("Carrier parameter must be provided.");
        }

        return switch (carrier.toLowerCase()) {
            case "firstnet" -> {
                Specification<FirstNetInventory> spec = InventorySpecifications.findByCriteria(filter);
                log.debug("Executing FirstNet inventory search with generic spec and pageable: {}", pageable);
                yield firstnetDao.findAll(spec, pageable).map(Function.identity());
            }
            case "at&t mobility" -> {
                Specification<ATTInventory> spec = InventorySpecifications.findByCriteria(filter);
                log.debug("Executing AT&T Mobility inventory search with generic spec and pageable: {}", pageable);
                yield attDao.findAll(spec, pageable).map(Function.identity());
            }
            default -> {
                log.error("Unsupported carrier specified: {}", carrier);
                throw new IllegalArgumentException("Unsupported carrier for filtering: " + carrier);
            }
        };
    }
}
