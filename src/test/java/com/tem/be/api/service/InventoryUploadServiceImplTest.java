package com.tem.be.api.service;

import com.tem.be.api.enums.FileType;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InventoryProcessingException;
import com.tem.be.api.model.InventoryHistory;
import com.tem.be.api.model.TempInventoryBase;
import com.tem.be.api.service.processors.InventoryProcessor;
import com.tem.be.api.service.processors.InventoryProcessorFactory;
import com.tem.be.api.utils.FileParsingUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link InventoryUploadServiceImpl} class.
 * <p>
 * This test class isolates the service's orchestration logic by mocking all its dependencies.
 * It verifies that the service correctly handles file processing workflows, delegates tasks to
 * the appropriate components (parser, factory, processor), and manages the process state
 * through the InventoryHistoryService.
 */
@ExtendWith(MockitoExtension.class)
class InventoryUploadServiceImplTest {

    private static final String CARRIER = "FirstNet";
    private static final String UPLOADED_BY = "testUser";

    //<editor-fold desc="Mocks and SUT">
    @Mock
    private InventoryHistoryService inventoryHistoryService;

    @Mock
    private FileParsingUtil fileParsingUtil;

    @Mock
    private AccountDepartmentMappingService departmentMappingService;

    @Mock
    private InventoryProcessorFactory processorFactory;

    @Mock
    private InventoryProcessor<TempInventoryBase> mockProcessor; // A generic mock processor

    @InjectMocks
    private InventoryUploadServiceImpl inventoryUploadService;
    //</editor-fold>

    //<editor-fold desc="Test Data">
    private MockMultipartFile csvFile;
    private MockMultipartFile xlsxFile;
    private InventoryHistory mockHistory;
    //</editor-fold>

    /**
     * Sets up common test data and mock behaviors before each test.
     */
    @BeforeEach
    void setUp() {
        csvFile = new MockMultipartFile(
                "file", "inventory.csv", "text/csv", "col1,col2\nval1,val2".getBytes()
        );
        xlsxFile = new MockMultipartFile(
                "file", "inventory.xlsx", "application/vnd.ms-excel", "excel-data".getBytes()
        );

        mockHistory = new InventoryHistory();
        mockHistory.setBatchId(UUID.randomUUID().toString());
        mockHistory.setStatus(InvoiceStatus.PENDING_APPROVAL);
    }

    /**
     * Tests the happy path for a successful CSV file processing workflow.
     * Verifies that all dependencies are called in the correct order and the process completes successfully.
     */
    @Test
    @DisplayName("processInventoryFile - Should succeed for a valid CSV file")
    void processInventoryFile_withValidCsvFile_shouldSucceed() throws Exception {
        // Arrange
        List<Map<String, String>> parsedData = List.of(Map.of("key", "value"));
        when(inventoryHistoryService.createInventoryHistory(any(InventoryHistory.class))).thenReturn(mockHistory);
        when(fileParsingUtil.readCsv(any(InputStream.class), eq(CARRIER), eq(FileType.INVENTORY))).thenReturn(parsedData);
        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);
        when(departmentMappingService.getDepartmentMapping(CARRIER)).thenReturn(Collections.emptyMap());
        when(mockProcessor.convertAndMapData(anyList(), any(), anyMap(), anyString())).thenReturn(Collections.emptyList());
        doNothing().when(mockProcessor).saveTempInventory(anyList());

        // Act
        String batchId = inventoryUploadService.processInventoryFile(csvFile, CARRIER, UPLOADED_BY);

        // Assert
        assertThat(batchId).isNotNull();
        verify(inventoryHistoryService).createInventoryHistory(any(InventoryHistory.class));
        verify(fileParsingUtil).readCsv(any(InputStream.class), eq(CARRIER), eq(FileType.INVENTORY));
        verify(processorFactory).getProcessor(CARRIER);
        verify(departmentMappingService).getDepartmentMapping(CARRIER);
        verify(mockProcessor).convertAndMapData(anyList(), any(), anyMap(), anyString());
        verify(mockProcessor).saveTempInventory(anyList());
        verify(inventoryHistoryService, never()).markAsFailed(anyString(), anyString());
    }

    /**
     * Tests the happy path for a successful XLSX file processing workflow.
     */
    @Test
    @DisplayName("processInventoryFile - Should succeed for a valid XLSX file")
    void processInventoryFile_withValidXlsxFile_shouldSucceed() throws Exception {
        // Arrange
        List<Map<String, String>> parsedData = List.of(Map.of("key", "value"));
        when(inventoryHistoryService.createInventoryHistory(any(InventoryHistory.class))).thenReturn(mockHistory);
        when(fileParsingUtil.readXlsx(any(InputStream.class), eq(CARRIER), eq(FileType.INVENTORY))).thenReturn(parsedData);
        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);

        // Act
        String batchId = inventoryUploadService.processInventoryFile(xlsxFile, CARRIER, UPLOADED_BY);

        // Assert
        assertThat(batchId).isNotNull();
        verify(fileParsingUtil).readXlsx(any(InputStream.class), eq(CARRIER), eq(FileType.INVENTORY));
        verify(inventoryHistoryService, never()).markAsFailed(anyString(), anyString());
    }

    /**
     * Tests that an exception is thrown and the history is marked as failed when the file parser returns empty data.
     */
    @Test
    @DisplayName("processInventoryFile - Should fail when parsed data is empty")
    void processInventoryFile_whenDataIsEmpty_shouldFailAndMarkHistory() throws Exception {
        // Arrange
        when(inventoryHistoryService.createInventoryHistory(any(InventoryHistory.class))).thenReturn(mockHistory);
        when(fileParsingUtil.readCsv(any(InputStream.class), anyString(), any())).thenReturn(Collections.emptyList());

        // Act & Assert
        InventoryProcessingException exception = assertThrows(InventoryProcessingException.class, () ->
                inventoryUploadService.processInventoryFile(csvFile, CARRIER, UPLOADED_BY)
        );

        assertThat(exception.getMessage()).contains("Failed to process inventory file");
        assertThat(exception.getCause().getMessage()).contains("The provided file is empty");

        verify(inventoryHistoryService).markAsFailed(anyString(), contains("file is empty"));
        verify(processorFactory, never()).getProcessor(anyString());
    }

    /**
     * Tests that an exception is thrown and the history is marked as failed when the FileParsingUtil throws an IOException.
     */
    @Test
    @DisplayName("processInventoryFile - Should fail when file parsing throws IOException")
    void processInventoryFile_whenParsingFails_shouldFailAndMarkHistory() throws Exception {
        // Arrange
        when(inventoryHistoryService.createInventoryHistory(any(InventoryHistory.class))).thenReturn(mockHistory);
        when(fileParsingUtil.readCsv(any(InputStream.class), anyString(), any())).thenThrow(new IOException("Corrupted file"));

        // Act & Assert
        InventoryProcessingException exception = assertThrows(InventoryProcessingException.class, () ->
                inventoryUploadService.processInventoryFile(csvFile, CARRIER, UPLOADED_BY)
        );

        assertThat(exception.getCause()).isInstanceOf(IOException.class);
        verify(inventoryHistoryService).markAsFailed(anyString(), eq("Corrupted file"));
    }

    /**
     * Tests that an exception is thrown when the filename is null, which is a preliminary check.
     */
    @Test
    @DisplayName("processInventoryFile - Should fail for a file with no extension")
    void processInventoryFile_withEmptyFilename_shouldFail() {
        // Arrange
        // This actually creates a file with an empty string "" as its name, not null.
        MockMultipartFile fileWithEmptyName = new MockMultipartFile("file", "", "text/csv", "data".getBytes());
        when(inventoryHistoryService.createInventoryHistory(any(InventoryHistory.class))).thenReturn(mockHistory);


        // Act & Assert
        // The inner exception is now caught and wrapped. We expect the outer exception.
        InventoryProcessingException exception = assertThrows(InventoryProcessingException.class, () ->
                inventoryUploadService.processInventoryFile(fileWithEmptyName, CARRIER, UPLOADED_BY)
        );

        // Optionally, assert the cause to be more specific
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getCause().getMessage()).contains("Unsupported file type");

        // Verify history was created then marked as failed
        verify(inventoryHistoryService).createInventoryHistory(any(InventoryHistory.class));
        verify(inventoryHistoryService).markAsFailed(anyString(), anyString());
    }
}
