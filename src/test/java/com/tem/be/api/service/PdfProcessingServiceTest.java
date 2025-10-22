package com.tem.be.api.service;

import com.tem.be.api.dao.WiredReportsDao;
import com.tem.be.api.model.WiredReports;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PdfProcessingService}.
 * <p>
 * This test class isolates the complex parsing logic from the actual PDF file I/O
 * by mocking the underlying {@code PDFBox} and {@code tabula-java} libraries.
 * The primary strategy is to use Mockito's {@code mockConstruction} feature to control
 * the output of these libraries, allowing the service's text-processing and state-machine
 * logic to be tested in isolation.
 */
@ExtendWith(MockitoExtension.class)
class PdfProcessingServiceTest {

    /**
     * Mocked DAO to verify that the service correctly calls the persistence layer
     * with the parsed data, and to capture the results for assertion.
     */
    @Mock
    private WiredReportsDao wiredReportsRepository;

    // --- Mocks for PDFBox and Tabula Object Model ---

    @Mock
    private PDDocument mockDocument;
    @Mock
    private Page mockPage;
    @Mock
    private Table mockTable;

    /**
     * The service instance under test, with its mocked dependencies injected.
     */
    @InjectMocks
    private PdfProcessingService pdfProcessingService;

    /**
     * Manages the static mock of {@code PDDocument.load(File)} to prevent
     * any actual file system access during tests.
     */
    private MockedStatic<PDDocument> pdDocumentMockedStatic;

    /**
     * Captures the list of {@link WiredReports} sent to the DAO's {@code saveAll} method,
     * allowing for detailed assertions on the final parsed objects.
     */
    @Captor
    private ArgumentCaptor<List<WiredReports>> reportsCaptor;

    /**
     * A concrete implementation of {@link RectangularTextContainer} for use in tests,
     * since the original class is abstract.
     */
    static class MockRectangularTextContainer extends RectangularTextContainer<MockRectangularTextContainer> {
        private final String text;

        public MockRectangularTextContainer(String text) {
            super(0, 0, 0, 0);
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }
    }

    /**
     * Helper method to create a mock {@code Tabula} row from a simple string.
     *
     * @param text The full text content of the row.
     * @return A list containing a single mock text container, simulating a table row.
     */
    @SuppressWarnings("rawtypes")
    private List<RectangularTextContainer> mockRow(String text) {
        return List.of(new MockRectangularTextContainer(text));
    }

    /**
     * Sets up mocks that are common across most test cases.
     * This includes mocking the static {@code PDDocument.load} method and providing a
     * lenient stub for {@code getNumberOfPages}.
     */
    @BeforeEach
    void setUp() {
        pdDocumentMockedStatic = mockStatic(PDDocument.class);
        pdDocumentMockedStatic.when(() -> PDDocument.load(any(File.class))).thenReturn(mockDocument);

        // This stub is made lenient because some tests (e.g., the no-header test) exit
        // before this method is ever called, which would otherwise cause an UnnecessaryStubbingException.
        lenient().when(mockDocument.getNumberOfPages()).thenReturn(1);
    }

    /**
     * Cleans up static mocks after each test to prevent state leakage between tests.
     */
    @AfterEach
    void tearDown() {
        pdDocumentMockedStatic.close();
    }

    /**
     * Tests that {@code processReport} can successfully parse a document with a valid header
     * and multiple types of data rows, and then saves the correctly formed objects.
     */
    @Test
    @DisplayName("processReport - Should fully parse complex report and save")
    @SuppressWarnings("rawtypes")
    void processReport_withFullData_shouldParseAndSave() {
        // Arrange
        // Use try-with-resources for mockConstruction. This ensures the mocks for constructors
        // are active only for this test and are automatically cleaned up afterward.
        try (@SuppressWarnings("unused") MockedConstruction<ObjectExtractor> mockedOE = mockConstruction(ObjectExtractor.class, (mock, context) -> when(mock.extract(anyInt())).thenReturn(mockPage));
             @SuppressWarnings("unused") MockedConstruction<BasicExtractionAlgorithm> mockedBEA = mockConstruction(BasicExtractionAlgorithm.class, (mock, context) -> when(mock.extract(mockPage)).thenReturn(List.of(mockTable)));
             @SuppressWarnings("unused") MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class, (mock, context) -> {
                 String headerText = """
                         Some text...
                         Invoice Number 12345678
                         Invoice Date 01/01/2023
                         Billing Acct Nbr (BAN) 987654321
                         Detail of Charges
                         """;
                 when(mock.getText(mockDocument)).thenReturn(headerText);
             })) {
            // Define the mock table data that the service will process.
            List<List<RectangularTextContainer>> rows = List.of(
                    mockRow("BTN: 123-456-7890 | Main Office"),
                    mockRow("Svc ID : 111222333A | 444 | 123 Main St, Anytown, CA 90210"),
                    mockRow("1 Y AT&T | Product A 1 01:30:00 10.00 10.00 01/01/23 MRC")
            );
            when(mockTable.getRows()).thenReturn(rows);

            // Act
            pdfProcessingService.processReport(new File("dummy.pdf"));

            // Assert
            verify(wiredReportsRepository).saveAll(reportsCaptor.capture());
            List<WiredReports> savedReports = reportsCaptor.getValue();

            assertThat(savedReports).hasSize(1);
            WiredReports report = savedReports.get(0);

            // Verify data from both the mocked header and the mocked table rows
            assertThat(report.getInvoiceNumber()).isEqualTo("000012345678");
            assertThat(report.getBan()).isEqualTo("987654321");
            assertThat(report.getInvoiceDate()).isEqualTo(LocalDate.of(2023, 1, 1));
            assertThat(report.getItemNumber()).isEqualTo("1");
            assertThat(report.getChargeType()).isEqualTo("MRC");
        }
    }

    /**
     * Tests that if the header information cannot be parsed from the first page,
     * the process stops gracefully without attempting to parse details or save to the repository.
     */
    @Test
    @DisplayName("processReport - When header is not found, should log error and not save")
    void processReport_whenNoHeader_shouldNotSave() {
        // Arrange
        // We control the outcome by providing text that the service's regex will fail to parse.
        try (@SuppressWarnings("unused") MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class, (mock, context) -> {
            String invalidHeaderText = """
                    Some text...
                    Invoice Number is missing
                    Invoice Date is missing
                    BAN is missing
                    """;
            when(mock.getText(mockDocument)).thenReturn(invalidHeaderText);
        })) {
            // Act
            pdfProcessingService.processReport(new File("dummy.pdf"));

            // Assert
            verify(wiredReportsRepository, never()).saveAll(any());
        }
    }

    /**
     * Tests that if a document contains a valid header but no actual charge item rows,
     * the process completes without saving any records to the repository.
     */
    @Test
    @DisplayName("processReport - When only context/header rows exist, should not save")
    @SuppressWarnings("rawtypes")
    // Necessary because the tabula-java library's getRows() method returns a raw List type.
    void processReport_whenNoChargeItems_shouldNotSave() {
        // Arrange
        // Provide a valid header, but mock the table to return only non-charge items.
        try (@SuppressWarnings("unused") MockedConstruction<ObjectExtractor> mockedOE = mockConstruction(ObjectExtractor.class, (mock, context) -> when(mock.extract(anyInt())).thenReturn(mockPage));
             @SuppressWarnings("unused") MockedConstruction<BasicExtractionAlgorithm> mockedBEA = mockConstruction(BasicExtractionAlgorithm.class, (mock, context) -> when(mock.extract(mockPage)).thenReturn(List.of(mockTable)));
             @SuppressWarnings("unused") MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class, (mock, context) -> {
                 String headerText = """
                         Invoice Number 12345
                         Invoice Date 01/01/2023
                         Billing Acct Nbr (BAN) 98765
                         Detail of Charges
                         """;
                 when(mock.getText(mockDocument)).thenReturn(headerText);
             })) {
            List<List<RectangularTextContainer>> rows = List.of(
                    mockRow("BTN: 123-456-7890 | Main Office"),
                    mockRow("Item # Contract Provider Product ID | Feature Name"),
                    mockRow("Svc ID : 111222333A | 444")
            );
            when(mockTable.getRows()).thenReturn(rows);

            // Act
            pdfProcessingService.processReport(new File("dummy.pdf"));

            // Assert
            verify(wiredReportsRepository, never()).saveAll(any());
        }
    }

    /**
     * Tests the robustness of the address parsing logic for an edge case where the city is not
     * separated by a comma.
     */
    @Test
    @DisplayName("processReport - Should correctly parse address with missing city comma")
    @SuppressWarnings("rawtypes")
    void processReport_parsesAddressWithoutCityCorrectly() {
        // Arrange
        // Provide a valid header and specific table rows for this address edge case.
        try (@SuppressWarnings("unused") MockedConstruction<ObjectExtractor> mockedOE = mockConstruction(ObjectExtractor.class, (mock, context) -> when(mock.extract(anyInt())).thenReturn(mockPage));
             @SuppressWarnings("unused") MockedConstruction<BasicExtractionAlgorithm> mockedBEA = mockConstruction(BasicExtractionAlgorithm.class, (mock, context) -> when(mock.extract(mockPage)).thenReturn(List.of(mockTable)));
             @SuppressWarnings("unused") MockedConstruction<PDFTextStripper> mockedStripper = mockConstruction(PDFTextStripper.class, (mock, context) -> {
                 String headerText = """
                         Invoice Number 1
                         Invoice Date 10/21/2025
                         Billing Acct Nbr (BAN) 1
                         Detail of Charges
                         """;
                 when(mock.getText(mockDocument)).thenReturn(headerText);
             })) {
            List<List<RectangularTextContainer>> rows = List.of(
                    mockRow("Svc ID : 123 | | 123 Government Way Sacramento CA 95814"),
                    mockRow("1 Y AT&T | Item 1 1 1.00 1.00 01/01/24 MRC")
            );
            when(mockTable.getRows()).thenReturn(rows);

            // Act
            pdfProcessingService.processReport(new File("dummy.pdf"));

            // Assert
            verify(wiredReportsRepository).saveAll(reportsCaptor.capture());
            WiredReports result = reportsCaptor.getValue().get(0);

            assertThat(result.getSvcAddress1()).isEqualTo("123 Government Way");
            assertThat(result.getSvcCity()).isEqualTo("Sacramento");
            assertThat(result.getSvcState()).isEqualTo("CA");
            assertThat(result.getSvcZip()).isEqualTo("95814");
        }
    }
}
