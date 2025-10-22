package com.tem.be.api.service;

import com.tem.be.api.dao.WiredReportsDao;
import com.tem.be.api.model.WiredReports;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import technology.tabula.*;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for processing CALNET PDF reports.
 * It extracts detailed charge information from the PDF, parses it into a structured format,
 * and persists the records to the database.
 */
@Service
@Log4j2
public class PdfProcessingService {

    private final WiredReportsDao wiredReportsRepository;

    /**
     * A record to hold invoice-level data extracted from the first page of the PDF.
     */
    record InvoiceHeader(String invoiceNumber, LocalDate invoiceDate, String ban) {
    }

    /**
     * A stateful class to hold the current parsing context as the document is read row by row.
     * This includes the current BTN, Svc ID, and associated address information.
     */
    private static class ParsingContext {
        String btn;
        String btnDescription;
        String svcId;
        String node;
        String svcAddress1;
        String svcAddress2;
        String svcCity;
        String svcState;
        String svcZip;

        void clearSvcId() {
            this.svcId = null;
            this.node = null;
            this.svcAddress1 = null;
            this.svcAddress2 = null;
            this.svcCity = null;
            this.svcState = null;
            this.svcZip = null;
        }

        void clearBtn() {
            this.btn = null;
            this.btnDescription = null;
            clearSvcId();
        }
    }

    /**
     * Constructs the service with a repository for data persistence.
     *
     * @param wiredReportsRepository The DAO for saving parsed report data.
     */
    @Autowired
    public PdfProcessingService(WiredReportsDao wiredReportsRepository) {
        this.wiredReportsRepository = wiredReportsRepository;
    }

    /**
     * Main entry point for processing a single PDF file.
     * It orchestrates the reading, parsing, and saving of the report data.
     *
     * @param pdfFile The PDF file to be processed.
     */
    public void processReport(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            InvoiceHeader header = extractHeaderInfo(document);
            if (header == null) {
                log.error("Could not parse header information from PDF: {}", pdfFile.getName());
                return;
            }
            log.info("Processing CALNET Report. BAN: {}, Invoice Number: {}, Date: {}", header.ban, header.invoiceNumber, header.invoiceDate);
            List<WiredReports> reports = parseDetailOfCharges(document, header);
            if (!reports.isEmpty()) {
                wiredReportsRepository.saveAll(reports);
                log.info("Successfully parsed and saved {} records from CALNET report {}.", reports.size(), header.invoiceNumber());
            } else {
                log.warn("No detailed charge items found in CALNET report {}.", header.invoiceNumber());
            }
        } catch (IOException e) {
            log.error("Failed to process CALNET PDF file: {}", pdfFile.getName(), e);
        }
    }

    /**
     * Extracts high-level invoice data (BAN, Invoice Number, Date) from the first page of the document.
     *
     * @param document The loaded PDDocument.
     * @return An {@link InvoiceHeader} record containing the extracted data, or null if any field is not found.
     * @throws IOException if there is an error reading the document.
     */
    InvoiceHeader extractHeaderInfo(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text = stripper.getText(document);
        Pattern banPattern = Pattern.compile("Billing Acct Nbr \\(BAN\\)\\s+(\\d+)");
        Pattern invNumPattern = Pattern.compile("Invoice Number\\s+(\\d+)");
        // Use concise character class \d instead of [0-9]
        Pattern invDatePattern = Pattern.compile("Invoice Date\\s+(\\d{2}/\\d{2}/\\d{4})");
        Matcher banMatcher = banPattern.matcher(text);
        Matcher invNumMatcher = invNumPattern.matcher(text);
        Matcher invDateMatcher = invDatePattern.matcher(text);
        if (banMatcher.find() && invNumMatcher.find() && invDateMatcher.find()) {
            return new InvoiceHeader(
                    String.format("%012d", Long.parseLong(invNumMatcher.group(1))),
                    LocalDate.parse(invDateMatcher.group(1), DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    banMatcher.group(1)
            );
        }
        return null;
    }

    /**
     * Iterates through the pages of the PDF, extracts tables, and processes each row
     * to build a list of charge records.
     *
     * @param document The loaded PDDocument.
     * @param header   The invoice header data.
     * @return A list of populated {@link WiredReports} objects.
     * @throws IOException if there is an error reading the document.
     */
    @SuppressWarnings("rawtypes")
    private List<WiredReports> parseDetailOfCharges(PDDocument document, InvoiceHeader header) throws IOException {
        List<WiredReports> reports = new ArrayList<>();
        ObjectExtractor oe = new ObjectExtractor(document);
        BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
        ParsingContext context = new ParsingContext();
        WiredReports currentReport = null;

        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            if (!isDetailPage(document, i)) {
                log.debug("Skipping page {} as it is not a 'Detail of Charges' page.", i);
                continue;
            }
            Page page = oe.extract(i);
            for (Table table : bea.extract(page)) {
                for (List<RectangularTextContainer> rowCells : table.getRows()) {
                    currentReport = handleRowProcessing(rowCells, context, currentReport, reports, header);
                }
            }
        }
        if (currentReport != null) {
            reports.add(currentReport);
        }
        return reports;
    }

    /**
     * Acts as a dispatcher, determining the type of a given row (charge item, context, or sub-row)
     * and calling the appropriate processing logic. This method helps reduce the cognitive complexity of the main loop.
     *
     * @param rowCells      The list of text containers for the current row.
     * @param context       The current parsing state.
     * @param currentReport The report item currently being built.
     * @param reports       The master list of completed reports.
     * @param header        The invoice header data.
     * @return The updated {@link WiredReports} item (which may be new, the same, or null).
     */
    @SuppressWarnings("rawtypes")
    private WiredReports handleRowProcessing(List<RectangularTextContainer> rowCells, ParsingContext context, WiredReports currentReport, List<WiredReports> reports, InvoiceHeader header) {
        String fullRowText = getFullRowText(rowCells).trim();
        if (fullRowText.isEmpty() || fullRowText.startsWith("Item #")) {
            return currentReport; // Ignore empty or header rows
        }

        if (isChargeItemRow(rowCells)) {
            log.debug("Processing as Charge Item Row: {}", fullRowText);
            if (currentReport != null) reports.add(currentReport);
            return createReportFromRow(rowCells, header, context);
        }

        if (isContextRow(fullRowText)) {
            log.debug("Processing as Context Row: {}", fullRowText);
            if (currentReport != null) reports.add(currentReport);
            updateContextForRowType(rowCells, fullRowText, context);
            return null; // A context row resets the current report
        }

        // Otherwise, it's a sub-row for the current report.
        if (currentReport != null) {
            log.debug("Processing as Sub-Row: {}", fullRowText);
            processSubRow(fullRowText, currentReport);
        }
        return currentReport;
    }

    /**
     * Updates the parsing context based on the type of context row detected.
     *
     * @param rowCells    The row's cells.
     * @param fullRowText The full text of the row.
     * @param context     The parsing context to update.
     */
    @SuppressWarnings("rawtypes")
    private void updateContextForRowType(List<RectangularTextContainer> rowCells, String fullRowText, ParsingContext context) {
        if (isBtnRow(fullRowText)) {
            context.clearBtn();
            updateBtnContext(rowCells, context);
        } else if (isSvcIdRow(fullRowText)) {
            updateSvcIdContext(rowCells, context);
        } else if (fullRowText.contains("Charges & Adjustments")) {
            context.clearSvcId();
            context.svcId = fullRowText;
        }
    }

    /**
     * Checks if a row is a context-setting row (e.g., starts with "BTN:", "Svc ID:", etc.).
     *
     * @param fullRowText The text of the row.
     * @return True if it is a context row, false otherwise.
     */
    private boolean isContextRow(String fullRowText) {
        return isBtnRow(fullRowText) || isSvcIdRow(fullRowText) || fullRowText.contains("Charges & Adjustments");
    }

    //<editor-fold desc="Core Parsing Logic">

    /**
     * Parses a single row that represents a main charge item.
     * This method works by "peeling off" known data from the right side of the string first,
     * which is more reliable than parsing left-to-right.
     *
     * @param rowCells The row's cells.
     * @param header   The invoice header data.
     * @param context  The current parsing context.
     * @return A new, populated {@link WiredReports} object, or null if parsing fails.
     */
    @SuppressWarnings("rawtypes")
    private WiredReports createReportFromRow(List<RectangularTextContainer> rowCells, InvoiceHeader header, ParsingContext context) {
        WiredReports report = new WiredReports();
        populateInitialData(report, header, context);

        String fullRowText = getFullRowText(rowCells).replaceAll("[\r\n]", " ").trim();
        String chargeTypeRegex = "(ADJ|NRC|PRC|MRC|PMT|SUR|TAX|USG)";

        // Phase 1: Extract the item number from the start.
        Pattern itemPattern = Pattern.compile("^(\\d+)\\s+(.*)");
        Matcher itemMatcher = itemPattern.matcher(fullRowText);
        if (!itemMatcher.matches()) {
            log.warn("Could not parse item number from row: {}", fullRowText);
            return null;
        }
        report.setItemNumber(itemMatcher.group(1));
        String remainingText = itemMatcher.group(2).trim();

        // Phase 2: Peel off known, right-aligned data from the end of the string.
        // This is more reliable as descriptions on the left can have variable formats.
        Pattern chargeTypePattern = Pattern.compile("(.*?)\\s+" + chargeTypeRegex + "$");
        Matcher chargeTypeMatcher = chargeTypePattern.matcher(remainingText);
        if (chargeTypeMatcher.matches()) {
            report.setChargeType(chargeTypeMatcher.group(2));
            remainingText = chargeTypeMatcher.group(1).trim();
        }

        Pattern billPeriodPattern = Pattern.compile("(.*?)\\s+(\\d{2}/\\d{2}/\\d{2,4})$");
        Matcher billPeriodMatcher = billPeriodPattern.matcher(remainingText);
        if (billPeriodMatcher.matches()) {
            report.setBillPeriod(billPeriodMatcher.group(2));
            remainingText = billPeriodMatcher.group(1).trim();
        }

        Pattern amountPattern = Pattern.compile("(.*?)\\s+(-?[\\d,]+\\.\\d{2})$");
        Matcher totalChargeMatcher = amountPattern.matcher(remainingText);
        if (totalChargeMatcher.matches()) {
            report.setTotalCharge(new BigDecimal(totalChargeMatcher.group(2).replace(",", "")));
            remainingText = totalChargeMatcher.group(1).trim();
        }

        Matcher contractRateMatcher = amountPattern.matcher(remainingText);
        if (contractRateMatcher.matches()) {
            report.setContractRate(new BigDecimal(contractRateMatcher.group(2).replace(",", "")));
            remainingText = contractRateMatcher.group(1).trim();
        }

        Pattern usagePattern = Pattern.compile("(.*?)\\s+(\\d{2}:\\d{2}:\\d{2})$");
        Matcher usageMatcher = usagePattern.matcher(remainingText);
        if (usageMatcher.matches()) {
            report.setMinutes(convertTimeToMinutes(usageMatcher.group(2)));
            remainingText = usageMatcher.group(1).trim();
        }

        Pattern qtyPattern = Pattern.compile("(.*?)\\s+(\\d+)$");
        Matcher qtyMatcher = qtyPattern.matcher(remainingText);
        if (qtyMatcher.matches()) {
            report.setQuantity(Integer.parseInt(qtyMatcher.group(2)));
            remainingText = qtyMatcher.group(1).trim();
        }

        // Phase 3: Parse the remaining middle part for descriptive fields.
        if (remainingText.startsWith("Y ") || remainingText.startsWith("N ")) {
            report.setContract(remainingText.substring(0, 1));
            remainingText = remainingText.substring(2).trim();
        }

        Pattern providerPattern = Pattern.compile("^(AT&T(?:\\s+\\w+)?)\\s+(.*)");
        Matcher providerMatcher = providerPattern.matcher(remainingText);
        if (providerMatcher.matches()) {
            report.setProvider(providerMatcher.group(1));
            remainingText = providerMatcher.group(2).trim();
        }

        String[] productParts = remainingText.split("\\s*\\|\\s*", 2);
        if (productParts.length > 0) report.setProductId(productParts[0].trim());
        if (productParts.length > 1) report.setFeatureName(productParts[1].trim());

        return report;
    }

    /**
     * Processes a "sub-row," which is a continuation line for a charge item.
     * This can contain a time-based usage, a charge type, a provider, and/or more description.
     *
     * @param subRowText The full text of the continuation row.
     * @param report     The {@link WiredReports} object to which the data should be appended.
     */
    private void processSubRow(String subRowText, WiredReports report) {
        // Handle time-only sub-rows
        if (subRowText.matches("^\\d{2,}:\\d{2}:\\d{2}$")) {
            report.setMinutes(convertTimeToMinutes(subRowText));
            return;
        }

        String remainingText = extractChargeTypeFromSubRow(subRowText.trim(), report);
        appendProviderAndDescriptionFromSubRow(remainingText, report);
    }

    /**
     * Attempts to extract a charge type (e.g., MRC, SUR) from the end of a sub-row string.
     *
     * @param text   The sub-row text.
     * @param report The report to update if a charge type is found.
     * @return The remaining text after the charge type has been removed, or the original text if none was found.
     */
    private String extractChargeTypeFromSubRow(String text, WiredReports report) {
        String chargeTypeRegex = "(ADJ|NRC|PRC|MRC|PMT|SUR|TAX|USG)";
        Pattern chargeTypePattern = Pattern.compile("^(.*?)\\s+" + chargeTypeRegex + "$");
        Matcher ctMatcher = chargeTypePattern.matcher(text);

        if (ctMatcher.matches()) {
            // Only set the charge type if it hasn't been set from the main row already.
            if (report.getChargeType() == null) {
                report.setChargeType(ctMatcher.group(2));
            }
            return ctMatcher.group(1).trim();
        }
        return text;
    }

    /**
     * Parses the remaining text from a sub-row to find a provider and append the rest to the description.
     *
     * @param text   The remaining text from a sub-row.
     * @param report The report to update.
     */
    private void appendProviderAndDescriptionFromSubRow(String text, WiredReports report) {
        if (text.isEmpty()) {
            return;
        }

        Pattern providerPattern = Pattern.compile("^(AT&T(?:\\s+\\w+)?)\\s*(.*)");
        Matcher providerMatcher = providerPattern.matcher(text);
        String currentDesc = report.getDescription() == null ? "" : report.getDescription() + "\n";

        if (providerMatcher.matches()) {
            // Only set the provider if not already set.
            if (report.getProvider() == null) {
                report.setProvider(providerMatcher.group(1).trim());
            }
            String descriptionPart = providerMatcher.group(2).trim();
            if (!descriptionPart.isEmpty()) {
                report.setDescription(currentDesc + descriptionPart);
            }
        } else {
            // If no provider is matched, the whole string is part of the description.
            report.setDescription(currentDesc + text);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Context and Helper Methods">

    /**
     * Populates a new report object with data from the current parsing context and invoice header.
     *
     * @param report  The {@link WiredReports} object to populate.
     * @param header  The invoice header data.
     * @param context The current parsing context.
     */
    private void populateInitialData(WiredReports report, InvoiceHeader header, ParsingContext context) {
        report.setCarrier("CALNET");
        report.setSection("Detail of Charges");
        report.setInvoiceNumber(header.invoiceNumber());
        report.setInvoiceDate(header.invoiceDate());
        report.setBan(header.ban());
        report.setSubgroup("No Subgroup");
        report.setBtn(context.btn);
        report.setBtnDescription(context.btnDescription);
        report.setSvcId(context.svcId);
        report.setNode(context.node);
        report.setSvcAddress1(context.svcAddress1);
        report.setSvcAddress2(context.svcAddress2);
        report.setSvcCity(context.svcCity);
        report.setSvcState(context.svcState);
        report.setSvcZip(context.svcZip);
    }

    /**
     * Parses a "BTN" row to update the BTN and its description in the parsing context.
     *
     * @param rowCells The cells of the BTN row.
     * @param context  The parsing context to update.
     */
    @SuppressWarnings("rawtypes")
    private void updateBtnContext(List<RectangularTextContainer> rowCells, ParsingContext context) {
        String btnText = getTextFromContextRow(rowCells);
        // Regex for BTN, pipe, and optional description.
        Pattern btnPattern = Pattern.compile("BTN:\\s+([^|]+)\\|?(.*)");
        Matcher matcher = btnPattern.matcher(btnText);
        if (matcher.find()) {
            context.btn = matcher.group(1).trim();
            context.btnDescription = matcher.group(2) != null ? matcher.group(2).trim() : null;
            log.debug("Context SET -> BTN: {}, Desc: {}", context.btn, context.btnDescription);
        }
    }

    /**
     * Parses a "Svc ID" row to update the Svc ID, node, and service address in the parsing context.
     *
     * @param rowCells The cells of the Svc ID row.
     * @param context  The parsing context to update.
     */
    @SuppressWarnings("rawtypes")
    private void updateSvcIdContext(List<RectangularTextContainer> rowCells, ParsingContext context) {
        String rawText = getTextFromContextRow(rowCells);
        context.clearSvcId();
        // This regex correctly handles cases with an ID, an optional node (even if empty), and an optional address.
        Pattern svcIdPattern = Pattern.compile("Svc ID :\\s+([^|]+?)\\s*(?:\\|\\s*(\\d*)\\s*(?:\\|\\s*(.*))?)?$");
        Matcher matcher = svcIdPattern.matcher(rawText);

        if (matcher.find()) {
            context.svcId = matcher.group(1).trim();
            String nodeStr = matcher.group(2);
            context.node = (nodeStr != null && !nodeStr.isEmpty()) ? nodeStr.trim() : null;
            String addressString = matcher.group(3);
            if (addressString != null) {
                parseServiceAddress(addressString.trim(), context);
            }
            log.debug("Context SET -> Svc ID: {}, Node: {}, Address: {}", context.svcId, context.node, context.svcAddress1);
        }
    }

    /**
     * Parses a raw address string into structured address fields (address line 1, city, state, zip).
     *
     * @param addressString The raw address string to parse.
     * @param context       The parsing context whose address fields will be populated.
     */
    private void parseServiceAddress(String addressString, ParsingContext context) {
        String cleanAddress = addressString.replace("#", " ").replaceAll("[,\\s]+$", "").trim();
        if (cleanAddress.isEmpty()) return;

        // Strategy 1: Look for the most common format: [Address], [City], [State] [Zip]
        Pattern fullPattern = Pattern.compile("^(.*?),\\s*([^,]+?),\\s*([A-Z]{2})\\s+(\\d{5}(?:-\\d{4})?)$");
        Matcher m = fullPattern.matcher(cleanAddress);
        if (m.matches()) {
            context.svcAddress1 = m.group(1).trim();
            context.svcCity = m.group(2).trim();
            context.svcState = m.group(3).trim();
            context.svcZip = m.group(4).trim();
            return;
        }

        // Strategy 2: Fallback for other formats. Extract state and zip first.
        String remainingPart = cleanAddress;
        Pattern stateZipPattern = Pattern.compile("^(.*?)\\s+([A-Z]{2})\\s+(\\d{5}(?:-\\d{4})?)$");
        Matcher szMatcher = stateZipPattern.matcher(remainingPart);
        if (szMatcher.matches()) {
            remainingPart = szMatcher.group(1).trim();
            context.svcState = szMatcher.group(2);
            context.svcZip = szMatcher.group(3);
        }

        // Find city and address from what's left.
        int lastComma = remainingPart.lastIndexOf(',');
        if (lastComma != -1) {
            context.svcAddress1 = remainingPart.substring(0, lastComma).trim();
            context.svcCity = remainingPart.substring(lastComma + 1).trim();
        } else {
            int lastSpace = remainingPart.lastIndexOf(' ');
            if (lastSpace != -1) {
                context.svcAddress1 = remainingPart.substring(0, lastSpace).trim();
                context.svcCity = remainingPart.substring(lastSpace + 1).trim();
            } else {
                context.svcAddress1 = remainingPart;
            }
        }
    }

    boolean isDetailPage(PDDocument doc, int page) throws IOException {
        PDFTextStripper s = new PDFTextStripper();
        s.setStartPage(page);
        s.setEndPage(page);
        return s.getText(doc).contains("Detail of Charges");
    }

    @SuppressWarnings("rawtypes")
    private boolean isChargeItemRow(List<RectangularTextContainer> r) {
        if (r == null || r.isEmpty()) return false;
        String firstCellText = getCellText(r, 0);
        return firstCellText.matches("^\\d+.*");
    }

    private boolean isBtnRow(String fullRowText) {
        return fullRowText.startsWith("BTN:");
    }

    private boolean isSvcIdRow(String fullRowText) {
        return fullRowText.startsWith("Svc ID :");
    }

    @SuppressWarnings("rawtypes")
    private String getCellText(List<RectangularTextContainer> c, int i) {
        return (i < c.size()) ? c.get(i).getText().trim() : "";
    }

    @SuppressWarnings("rawtypes")
    private String getFullRowText(List<RectangularTextContainer> c) {
        return c.stream().map(RectangularTextContainer::getText).collect(Collectors.joining(" "));
    }

    private BigDecimal convertTimeToMinutes(String t) {
        if (t == null) return null;
        String[] p = t.split(":");
        if (p.length != 3) return null;
        try {
            double minutes = Double.parseDouble(p[0]) * 60 + Double.parseDouble(p[1]) + Double.parseDouble(p[2]) / 60.0;
            return BigDecimal.valueOf(minutes).setScale(4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private String getTextFromContextRow(List<RectangularTextContainer> rowCells) {
        String fullText = rowCells.stream()
                .map(c -> c.getText().replaceAll("[\r\n]", " ").trim())
                .collect(Collectors.joining(" ")).trim();

        // Remove trailing monetary amounts that sometimes get stuck to context lines.
        Pattern amountPattern = Pattern.compile("\\s+[\\d,]+\\.\\d{2}$");
        Matcher matcher = amountPattern.matcher(fullText);
        if (matcher.find()) {
            return fullText.substring(0, matcher.start());
        }
        return fullText;
    }
    //</editor-fold>
}
