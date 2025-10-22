package com.tem.be.api.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.search.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * A service that connects to an email account via IMAP, searches for emails
 * based on configured criteria (sender, date), finds PDF attachments within
 * those emails, downloads them to temporary files, and passes them to the
 * {@link PdfProcessingService} for further processing.
 */
@Service
@Log4j2
public class EmailCrawlerService {

    // --- Injected Configuration Properties ---
    // These values are loaded from the application.properties file.

    @Value("${email.crawler.host}")
    private String host;

    @Value("${email.crawler.port}")
    private int port;

    @Value("${email.crawler.protocol}")
    private String protocol;

    @Value("${email.crawler.username}")
    private String username;

    @Value("${email.crawler.password}")
    private String password;

    @Value("${email.crawler.folder}")
    private String folderName;

    @Value("${email.crawler.sender.filter}")
    private String senderFilter;

    @Value("${email.crawler.days.back}")
    private int daysBack;

    /**
     * The service responsible for parsing the content of the downloaded PDF files.
     */
    private final PdfProcessingService pdfProcessingService;

    /**
     * Constructs the EmailCrawlerService with its required dependency.
     *
     * @param pdfProcessingService An instance of {@link PdfProcessingService} to handle PDF files.
     */
    @Autowired
    public EmailCrawlerService(PdfProcessingService pdfProcessingService) {
        this.pdfProcessingService = pdfProcessingService;
    }

    /**
     * The main entry point for the email crawling process. It establishes a connection
     * to the mail server, searches the specified folder for relevant emails, and
     * initiates the processing of any found attachments.
     */
    public void findPdfAttachments() {
        Properties props = new Properties();
        // Set standard mail properties for the IMAP server connection.
        props.put(String.format("mail.%s.host", protocol), host);
        props.put(String.format("mail.%s.port", protocol), port);
        props.put("mail.store.protocol", protocol);
        // Enable SSL for a secure, encrypted connection to the mail server.
        props.put(String.format("mail.%s.ssl.enable", protocol), "true");

        Session session = Session.getInstance(props, null);

        // Use try-with-resources to ensure the Store and Folder resources are automatically closed.
        try (Store store = session.getStore(protocol)) {
            log.info("Connecting to mail server: {}", host);
            store.connect(host, username, password);
            log.info("Connection successful.");

            try (Folder emailFolder = store.getFolder(folderName)) {
                // Open the folder in READ_ONLY mode to prevent any accidental modifications.
                emailFolder.open(Folder.READ_ONLY);

                log.info("Building search criteria for sender '{}' and emails from the last {} days.", senderFilter, daysBack);
                SearchTerm searchTerm = buildSearchTerm();

                // Perform the search. This is executed on the mail server for efficiency.
                Message[] messages = emailFolder.search(searchTerm);

                log.info("Found {} messages matching the criteria.", messages.length);

                if (messages.length == 0) {
                    log.warn("No emails found for sender '{}' in the last {} days.", senderFilter, daysBack);
                    return; // Exit if no messages are found.
                }

                // Iterate through each matched message and process it for attachments.
                for (Message message : messages) {
                    processMessage(message);
                }
            }

        } catch (NoSuchProviderException e) {
            log.error("Invalid mail protocol configured: {}. Check application.properties.", protocol, e);
        } catch (MessagingException e) {
            log.error("Could not connect to the mail server. Verify host, port, username, and password.", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during email processing.", e);
        }
    }

    /**
     * Constructs a {@link SearchTerm} object used to filter emails on the server side.
     * It combines multiple criteria (sender and date) with AND logic.
     *
     * @return A {@link SearchTerm} that represents the combined filtering rules.
     */
    private SearchTerm buildSearchTerm() {
        try {
            List<SearchTerm> terms = new ArrayList<>();

            // 1. Filter by the sender's email address.
            terms.add(new FromTerm(new InternetAddress(senderFilter)));

            // 2. Filter by date: find emails sent on or after a calculated start date.
            LocalDate sinceDate = LocalDate.now().minusDays(daysBack);
            Date since = Date.from(sinceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            // The comparison term GE means "greater than or equal to".
            terms.add(new SentDateTerm(ComparisonTerm.GE, since));

            // Combine all individual search terms into a single "AND" term.
            // An email must match ALL terms to be included in the result.
            return new AndTerm(terms.toArray(new SearchTerm[0]));

        } catch (Exception e) {
            log.error("Failed to build search term. Check the sender email format in properties.", e);
            // Return a term that will find nothing, failing safely.
            return new AndTerm(new SearchTerm[0]);
        }
    }

    /**
     * Processes an email message, searching for and handling PDF attachments.
     *
     * @param message The email message to process.
     * @throws MessagingException If there's an error accessing message content.
     * @throws IOException        If there's an error reading multipart data.
     */
    private void processMessage(Message message) throws MessagingException, IOException {
        // Attachments are part of a Multipart content type. If not multipart, skip.
        if (!(message.getContent() instanceof Multipart multipart)) {
            log.debug("Skipping message with non-multipart content type.");
            return;
        }

        // Iterate through each part of the multipart message.
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            // Check if the part is a valid PDF attachment and process it if it is.
            if (isPdfAttachment(bodyPart)) {
                processPdfAttachment(bodyPart, message);
            }
        }
    }

    /**
     * Checks if a BodyPart is a downloadable PDF attachment.
     *
     * @param bodyPart The message part to check.
     * @return true if the part is a PDF attachment, false otherwise.
     * @throws MessagingException if there's an error accessing the part's details.
     */
    private boolean isPdfAttachment(BodyPart bodyPart) throws MessagingException {
        String fileName = bodyPart.getFileName();
        return Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                fileName != null &&
                fileName.toLowerCase().endsWith(".pdf");
    }

    /**
     * Downloads a PDF attachment to a temporary file, sends it for processing,
     * and ensures the temporary file is cleaned up afterward.
     *
     * @param bodyPart The PDF attachment part of the message.
     * @param message  The parent email message, used for logging context.
     * @throws MessagingException if there's an error getting the attachment's file name.
     */
    private void processPdfAttachment(BodyPart bodyPart, Message message) throws MessagingException {
        log.info("---------------------------------");
        log.info("Found PDF Attachment!");
        log.info("Email From: {}", InternetAddress.toString(message.getFrom()));
        log.info("Email Subject: {}", message.getSubject());
        log.info("PDF File Name: {}", bodyPart.getFileName());

        File tempFile = null;
        try {
            // Create a temporary local file to store the PDF content.
            tempFile = Files.createTempFile("invoice-", ".pdf").toFile();

            // Use try-with-resources for the InputStream to ensure it's closed automatically.
            try (InputStream is = bodyPart.getInputStream()) {
                // Copy the attachment's input stream to our new temporary file.
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("PDF downloaded to temporary file: {}", tempFile.getAbsolutePath());
            }

            // Hand off the downloaded file to the processing service.
            pdfProcessingService.processReport(tempFile);

        } catch (Exception e) {
            log.error("An error occurred while processing attachment '{}'", bodyPart.getFileName(), e);
        } finally {
            // CRITICAL: Ensure the temporary file is deleted after processing.
            deleteTemporaryFile(tempFile);
            log.info("---------------------------------");
        }
    }

    /**
     * Safely deletes a file if it exists, logging the outcome.
     * Uses java.nio.file.Files for better error reporting.
     *
     * @param file The file to be deleted. Can be null.
     */
    private void deleteTemporaryFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
                log.info("Temporary file deleted successfully.");
            } catch (IOException e) {
                // Log with the exception for better diagnostics on why deletion failed (e.g., permissions).
                log.warn("Failed to delete temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}
