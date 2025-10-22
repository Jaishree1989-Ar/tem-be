package com.tem.be.api.service;

import com.tem.be.api.enums.FileType;
import com.tem.be.api.enums.InvoiceStatus;
import com.tem.be.api.exception.InvoiceProcessingException;
import com.tem.be.api.model.InvoiceHistory;
import com.tem.be.api.model.TempInvoiceBase;
import com.tem.be.api.service.processors.InvoiceProcessor;
import com.tem.be.api.service.processors.InvoiceProcessorFactory;
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
 * Unit tests for the {@link InvoiceUploadServiceImpl} class.
 * <p>
 * This test class isolates the service's orchestration logic by mocking all its dependencies.
 * It verifies that the service correctly handles file processing workflows, delegates tasks to
 * the appropriate components (parser, factory, processor), and manages the process state
 * through the InvoiceHistoryService.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceUploadServiceImplTest {

    private static final String CARRIER = "FirstNet";
    private static final String UPLOADED_BY = "testUser";

    //<editor-fold desc="Mocks and SUT">
    @Mock
    private InvoiceHistoryService invoiceHistoryService;

    @Mock
    private FileParsingUtil fileParsingUtil;

    @Mock
    private AccountDepartmentMappingService departmentMappingService;

    @Mock
    private InvoiceProcessorFactory processorFactory;

    @Mock
    private InvoiceProcessor<TempInvoiceBase> mockProcessor; // A generic mock processor

    @InjectMocks
    private InvoiceUploadServiceImpl invoiceUploadService;
    //</editor-fold>

    //<editor-fold desc="Test Data">
    private MockMultipartFile csvFile;
    private MockMultipartFile xlsxFile;
    private InvoiceHistory mockHistory;
    //</editor-fold>

    /**
     * Sets up common test data and mock behaviors before each test.
     */
    @BeforeEach
    void setUp() {
        csvFile = new MockMultipartFile(
                "file", "invoice.csv", "text/csv", "col1,col2\nval1,val2".getBytes()
        );
        xlsxFile = new MockMultipartFile(
                "file", "invoice.xlsx", "application/vnd.ms-excel", "excel-data".getBytes()
        );

        mockHistory = new InvoiceHistory();
        mockHistory.setBatchId(UUID.randomUUID().toString());
        mockHistory.setStatus(InvoiceStatus.PENDING_APPROVAL);
    }

    /**
     * Tests the happy path for a successful CSV file processing workflow.
     * Verifies that all dependencies are called in the correct order and the process completes successfully.
     */
    @Test
    @DisplayName("processInvoiceFile - Should succeed for a valid CSV file")
    void processInvoiceFile_withValidCsvFile_shouldSucceed() throws Exception {
        // Arrange
        List<Map<String, String>> parsedData = List.of(Map.of("key", "value"));
        when(invoiceHistoryService.createInvoiceHistory(any(InvoiceHistory.class))).thenReturn(mockHistory);
        when(fileParsingUtil.readCsv(any(InputStream.class), eq(CARRIER), eq(FileType.INVOICE))).thenReturn(parsedData);
        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);
        when(departmentMappingService.getDepartmentMapping(CARRIER)).thenReturn(Collections.emptyMap());
        when(mockProcessor.convertAndEnrichData(anyList(), any(InvoiceHistory.class), anyString(), anyMap())).thenReturn(Collections.emptyList());
        doNothing().when(mockProcessor).saveTempInvoices(anyList());

        // Act
        String batchId = invoiceUploadService.processDetailFile(csvFile, CARRIER, UPLOADED_BY);

        // Assert
        assertThat(batchId).isNotNull();
        verify(invoiceHistoryService).createInvoiceHistory(any(InvoiceHistory.class));
        verify(fileParsingUtil).readCsv(any(InputStream.class), eq(CARRIER), eq(FileType.INVOICE));
        verify(processorFactory).getProcessor(CARRIER);
        verify(departmentMappingService).getDepartmentMapping(CARRIER);
        verify(mockProcessor).convertAndEnrichData(anyList(), any(), anyString(), anyMap());
        verify(mockProcessor).saveTempInvoices(anyList());
        verify(invoiceHistoryService, never()).markAsFailed(anyString(), anyString());
    }

    /**
     * Tests the happy path for a successful XLSX file processing workflow.
     */
    @Test
    @DisplayName("processInvoiceFile - Should succeed for a valid XLSX file")
    void processInvoiceFile_withValidXlsxFile_shouldSucceed() throws Exception {
        // Arrange
        List<Map<String, String>> parsedData = List.of(Map.of("key", "value"));
        when(invoiceHistoryService.createInvoiceHistory(any(InvoiceHistory.class))).thenReturn(mockHistory);
        when(fileParsingUtil.readXlsx(any(InputStream.class), eq(CARRIER), eq(FileType.INVOICE))).thenReturn(parsedData);
        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);

        // Act
        String batchId = invoiceUploadService.processDetailFile(xlsxFile, CARRIER, UPLOADED_BY);

        // Assert
        assertThat(batchId).isNotNull();
        verify(fileParsingUtil).readXlsx(any(InputStream.class), eq(CARRIER), eq(FileType.INVOICE));
        verify(invoiceHistoryService, never()).markAsFailed(anyString(), anyString());
    }

    /**
     * Tests that an exception is thrown and the history is marked as failed when the file parser returns empty data.
     */
    @Test
    @DisplayName("processInvoiceFile - Should fail when parsed data is empty")
    void processInvoiceFile_whenDataIsEmpty_shouldFailAndMarkHistory() throws Exception {
        // Arrange
        when(invoiceHistoryService.createInvoiceHistory(any(InvoiceHistory.class))).thenReturn(mockHistory);
        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);
        when(fileParsingUtil.readCsv(any(InputStream.class), anyString(), any())).thenReturn(Collections.emptyList());

        // Act & Assert
        InvoiceProcessingException exception = assertThrows(InvoiceProcessingException.class, () ->
                invoiceUploadService.processDetailFile(csvFile, CARRIER, UPLOADED_BY)
        );

        assertThat(exception.getMessage()).startsWith("Failed to process file for batch");
        assertThat(exception.getCause().getMessage()).contains("The file is empty or contains no data rows.");

        verify(invoiceHistoryService).markAsFailed(anyString(), contains("file is empty"));
        verify(processorFactory).getProcessor(CARRIER);
    }

    /**
     * Tests that an exception is thrown and the history is marked as failed when the FileParsingUtil throws an IOException.
     */
    @Test
    @DisplayName("processInvoiceFile - Should fail when file parsing throws IOException")
    void processInvoiceFile_whenParsingFails_shouldFailAndMarkHistory() throws Exception {
        // Arrange
        when(invoiceHistoryService.createInvoiceHistory(any(InvoiceHistory.class))).thenReturn(mockHistory);
        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);
        when(fileParsingUtil.readCsv(any(InputStream.class), anyString(), any())).thenThrow(new IOException("Corrupted file"));

        // Act & Assert
        InvoiceProcessingException exception = assertThrows(InvoiceProcessingException.class, () ->
                invoiceUploadService.processDetailFile(csvFile, CARRIER, UPLOADED_BY)
        );

        assertThat(exception.getCause()).isInstanceOf(IOException.class);
        verify(invoiceHistoryService).markAsFailed(anyString(), eq("Corrupted file"));
    }

    /**
     * Tests that an exception is thrown when the filename is null, which is a preliminary check.
     */
    @Test
    @DisplayName("processInvoiceFile - Should fail for a file with no extension")
    void processInvoiceFile_withEmptyFilename_shouldFail() {
        // Arrange
        // This actually creates a file with an empty string "" as its name, not null.
        MockMultipartFile fileWithEmptyName = new MockMultipartFile("file", "", "text/csv", "data".getBytes());
        when(invoiceHistoryService.createInvoiceHistory(any(InvoiceHistory.class))).thenReturn(mockHistory);

        doReturn(mockProcessor).when(processorFactory).getProcessor(CARRIER);

        // Act & Assert
        // The inner exception is now caught and wrapped. We expect the outer exception.
        InvoiceProcessingException exception = assertThrows(InvoiceProcessingException.class, () ->
                invoiceUploadService.processDetailFile(fileWithEmptyName, CARRIER, UPLOADED_BY)
        );

        // Optionally, assert the cause to be more specific
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getCause().getMessage()).contains("Unsupported file type");

        // Verify history was created then marked as failed
        verify(invoiceHistoryService).createInvoiceHistory(any(InvoiceHistory.class));
        verify(invoiceHistoryService).markAsFailed(anyString(), anyString());
    }
}
