package com.tem.be.api.service;

import javax.mail.*;
import javax.mail.search.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailCrawlerService}.
 * <p>
 * This class mocks the entire JavaMail API interaction (Session, Store, Folder, etc.)
 * to test the service's logic without requiring a live email server connection. It also
 * mocks file system interactions to avoid creating temporary files during tests.
 */
@ExtendWith(MockitoExtension.class)
class EmailCrawlerServiceTest {

    /**
     * Mocked service for PDF processing, to verify that the crawler
     * correctly passes downloaded files for processing.
     */
    @Mock
    private PdfProcessingService pdfProcessingService;

    // --- Mocks for the JavaMail API ---

    @Mock
    private Session mockSession;
    @Mock
    private Store mockStore;
    @Mock
    private Folder mockFolder;
    @Mock
    private Message mockMessage;
    @Mock
    private Multipart mockMultipart;
    @Mock
    private BodyPart mockPdfBodyPart;

    /**
     * The service instance under test, with mocked dependencies injected.
     */
    @InjectMocks
    private EmailCrawlerService emailCrawlerService;

    /**
     * Manages static mocks for {@code Session.getInstance()} and {@code Files} methods.
     */
    private MockedStatic<Session> sessionMockedStatic;
    private MockedStatic<Files> filesMockedStatic;

    /**
     * Sets up configuration properties via reflection and initializes static mocks
     * before each test execution.
     */
    @BeforeEach
    void setUp() {
        // Use ReflectionTestUtils to inject config properties into the service instance
        ReflectionTestUtils.setField(emailCrawlerService, "host", "imap.test.com");
        ReflectionTestUtils.setField(emailCrawlerService, "port", 993);
        ReflectionTestUtils.setField(emailCrawlerService, "protocol", "imaps");
        ReflectionTestUtils.setField(emailCrawlerService, "username", "user@test.com");
        ReflectionTestUtils.setField(emailCrawlerService, "password", "password");
        ReflectionTestUtils.setField(emailCrawlerService, "folderName", "INBOX");
        ReflectionTestUtils.setField(emailCrawlerService, "senderFilter", "sender@test.com");
        ReflectionTestUtils.setField(emailCrawlerService, "daysBack", 30);

        // Mock the static Session.getInstance() method to return our mock session
        sessionMockedStatic = mockStatic(Session.class);
        sessionMockedStatic.when(() -> Session.getInstance(any(Properties.class), any())).thenReturn(mockSession);

        // Mock static Files methods to avoid real filesystem interaction
        filesMockedStatic = mockStatic(Files.class);
    }

    /**
     * Cleans up static mocks after each test to ensure test isolation.
     */
    @AfterEach
    void tearDown() {
        sessionMockedStatic.close();
        filesMockedStatic.close();
    }

    /**
     * Tests the happy path where a matching email with a PDF attachment is found,
     * downloaded, and passed to the processing service.
     */
    @Test
    @DisplayName("findPdfAttachments - Should process PDF when found")
    void findPdfAttachments_whenPdfFound_shouldProcess() throws Exception {
        // Arrange
        // --- Mocking the entire JavaMail API flow ---
        when(mockSession.getStore("imaps")).thenReturn(mockStore);
        doNothing().when(mockStore).connect(anyString(), anyString(), anyString());
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        doNothing().when(mockFolder).open(Folder.READ_ONLY);
        doNothing().when(mockFolder).close();
        doNothing().when(mockStore).close();
        when(mockFolder.search(any(SearchTerm.class))).thenReturn(new Message[]{mockMessage});

        // --- Mocking message content and attachment details ---
        when(mockMessage.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getCount()).thenReturn(1);
        when(mockMultipart.getBodyPart(0)).thenReturn(mockPdfBodyPart);
        when(mockPdfBodyPart.getDisposition()).thenReturn(Part.ATTACHMENT);
        when(mockPdfBodyPart.getFileName()).thenReturn("invoice.pdf");
        InputStream mockInputStream = new ByteArrayInputStream("pdf-content".getBytes());
        when(mockPdfBodyPart.getInputStream()).thenReturn(mockInputStream);

        // --- Mocking file system interaction ---
        File mockFile = mock(File.class);
        Path mockPath = mock(Path.class);
        when(mockFile.toPath()).thenReturn(mockPath);
        when(mockFile.exists()).thenReturn(true);
        filesMockedStatic.when(() -> Files.createTempFile(anyString(), anyString())).thenReturn(mockPath);
        when(mockPath.toFile()).thenReturn(mockFile);
        filesMockedStatic.when(() -> Files.copy(any(InputStream.class), any(Path.class), any())).thenReturn(1L);

        // Act
        emailCrawlerService.findPdfAttachments();

        // Assert
        verify(mockStore).connect("imap.test.com", "user@test.com", "password");
        verify(mockFolder).open(Folder.READ_ONLY);
        verify(mockFolder).search(any(SearchTerm.class));
        verify(pdfProcessingService).processReport(mockFile);
        verify(mockStore).close();
        verify(mockFolder).close();
        filesMockedStatic.verify(() -> Files.delete(mockPath)); // Verify temp file is deleted
    }

    /**
     * Tests that the service handles the case where no emails match the search criteria,
     * and exits gracefully without calling the PDF processor.
     */
    @Test
    @DisplayName("findPdfAttachments - Should do nothing when no messages are found")
    void findPdfAttachments_whenNoMessages_shouldDoNothing() throws MessagingException {
        // Arrange
        when(mockSession.getStore("imaps")).thenReturn(mockStore);
        doNothing().when(mockStore).connect(anyString(), anyString(), anyString());
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        doNothing().when(mockFolder).open(Folder.READ_ONLY);
        when(mockFolder.search(any(SearchTerm.class))).thenReturn(new Message[]{}); // Return empty array

        // Act
        emailCrawlerService.findPdfAttachments();

        // Assert
        verify(pdfProcessingService, never()).processReport(any());
        verify(mockStore).close();
        verify(mockFolder).close();
    }

    /**
     * Tests that messages found by the search are correctly skipped if they do not
     * contain a valid PDF attachment.
     */
    @Test
    @DisplayName("findPdfAttachments - Should skip message with no PDF attachment")
    void findPdfAttachments_whenNoPdf_shouldSkip() throws IOException, MessagingException {
        // Arrange
        when(mockSession.getStore("imaps")).thenReturn(mockStore);
        doNothing().when(mockStore).connect(anyString(), anyString(), anyString());
        when(mockStore.getFolder("INBOX")).thenReturn(mockFolder);
        doNothing().when(mockFolder).open(Folder.READ_ONLY);
        when(mockFolder.search(any(SearchTerm.class))).thenReturn(new Message[]{mockMessage});

        // Mock a non-attachment part (e.g., an inline image)
        BodyPart nonAttachmentPart = mock(BodyPart.class);
        when(nonAttachmentPart.getDisposition()).thenReturn(Part.INLINE);
        when(nonAttachmentPart.getFileName()).thenReturn("image.jpg");

        when(mockMessage.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getCount()).thenReturn(1);
        when(mockMultipart.getBodyPart(0)).thenReturn(nonAttachmentPart);

        // Act
        emailCrawlerService.findPdfAttachments();

        // Assert
        verify(pdfProcessingService, never()).processReport(any());
    }

    /**
     * Tests that a {@link MessagingException} during the connection phase is caught
     * and handled gracefully, preventing further processing.
     */
    @Test
    @DisplayName("findPdfAttachments - Should handle MessagingException on connect")
    void findPdfAttachments_whenConnectFails_shouldLogAndExit() throws MessagingException {
        // Arrange
        when(mockSession.getStore("imaps")).thenReturn(mockStore);
        doThrow(new MessagingException("Connection failed")).when(mockStore).connect(anyString(), anyString(), anyString());

        // Act
        emailCrawlerService.findPdfAttachments();

        // Assert
        verify(pdfProcessingService, never()).processReport(any());
        verify(mockStore, never()).getFolder(anyString()); // Verify that the logic inside the try block was skipped
    }
}
