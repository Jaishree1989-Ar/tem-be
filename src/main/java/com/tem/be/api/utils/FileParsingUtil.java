package com.tem.be.api.utils;

import com.tem.be.api.enums.FileType;
import com.tem.be.api.exception.ProviderConfigLoadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A utility component for parsing various file formats like XLSX, CSV, and PDF.
 * It uses provider-specific configurations to validate headers and map columns correctly.
 */
@Component
@Log4j2
public class FileParsingUtil {

    private static final String CONFIG_FILE_PATH = "provider_headers.json";
    private final Map<String, ProviderConfig> providerConfigs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor that loads provider configurations from a JSON file upon initialization.
     */
    public FileParsingUtil() {
        this.providerConfigs = loadProviderConfigs();
    }

    /**
     * Loads and parses the provider header configurations from a JSON resource file.
     *
     * @return A map where the key is the lowercase provider name and the value is its configuration.
     */
    private Map<String, ProviderConfig> loadProviderConfigs() {
        log.info("FileParsingUtil.loadProviderConfigs() >> Entry");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {
            if (is == null) {
                // Throw the custom exception when the file is not found.
                throw new ProviderConfigLoadException("Cannot find provider config resource file: " + CONFIG_FILE_PATH);
            }
            Map<String, ProviderConfig> configs = objectMapper.readValue(is, new TypeReference<>() {
            });

            // Normalize keys to lowercase for case-insensitive lookup later.
            Map<String, ProviderConfig> normalizedConfigs = configs.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toLowerCase(),
                            Map.Entry::getValue
                    ));
            log.info("FileParsingUtil.loadProviderConfigs() >> Exited Successfully | Loaded {} provider configs.", normalizedConfigs.size());
            return normalizedConfigs;
        } catch (IOException e) {
            log.error("Failed to load or parse provider configs from: {}", CONFIG_FILE_PATH, e);
            // Throw the custom exception, wrapping the original IOException.
            throw new ProviderConfigLoadException("Failed to load or parse provider configs: " + CONFIG_FILE_PATH, e);
        }
    }

    /**
     * Reads an XLSX file, validates its headers against the provider configuration,
     * and returns the data as a list of maps.
     *
     * @param inputStream The input stream of the XLSX file.
     * @param provider    The name of the provider for header validation.
     * @param fileType    The type of file invoice or inventory
     * @return A list where each map represents a row, with column headers as keys.
     * @throws IOException if there is an error reading the file.
     */
    public List<Map<String, String>> readXlsx(InputStream inputStream, String provider, FileType fileType) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return rows;
            }

            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.toString().trim());
            }

            validateHeaders(headers, provider, fileType);

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowData = new HashMap<>();

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    // This is the best way to get the value as it appears in Excel
                    String value = formatter.formatCellValue(cell, evaluator).trim();

                    rowData.put(headers.get(i), value);
                }

                // Add the populated map to the list if it's not an empty row
                if (rowData.values().stream().anyMatch(val -> val != null && !val.isBlank())) {
                    rows.add(rowData);
                }
            }
        } catch (POIXMLException | InvalidFormatException e) {
            // This block catches errors specific to invalid file formats or corruption
            log.error("Failed to parse XLSX file, it is likely corrupted or not a valid XLSX format. Error: {}", e.getMessage());
            throw new IOException("The file is corrupted or not a valid Excel (XLSX) file.", e);
        }
        return rows;
    }

    /**
     * Reads a CSV file, validates its headers, and returns the data as a list of maps.
     *
     * @param inputStream The input stream of the CSV file.
     * @param provider    The name of the provider for header validation.
     * @return A list where each map represents a row, with column headers as keys.
     * @throws IOException if there is an error reading the file.
     */
    public List<Map<String, String>> readCsv(InputStream inputStream, String provider, FileType fileType) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVReader reader = new CSVReader(br)) {

            String[] headersArray = reader.readNext();
            if (headersArray == null) {
                return rows;
            }

            List<String> headers = new ArrayList<>(Arrays.asList(headersArray));
            // Handle BOM character
            if (!headers.isEmpty() && headers.get(0).startsWith("\uFEFF")) {
                headers.set(0, headers.get(0).substring(1));
            }
            validateHeaders(headers, provider, fileType);

            String[] cleanHeaders = headers.toArray(new String[0]);
            String[] line;

            while ((line = reader.readNext()) != null) {
                Map<String, String> rowData = new HashMap<>();

                for (int i = 0; i < cleanHeaders.length && i < line.length; i++) {
                    // The cleanExcelValue method is fine here
                    rowData.put(cleanHeaders[i].trim(), cleanExcelValue(line[i]));
                }

                if (!rowData.isEmpty() && rowData.values().stream().anyMatch(val -> val != null && !val.isBlank())) {
                    rows.add(rowData);
                }
            }
        } catch (CsvValidationException e) {
            // This error indicates a structural problem with the CSV file
            log.error("CSV validation failed on line {}: {}", e.getLineNumber(), e.getMessage());
            throw new IOException("The CSV file is malformed or corrupted near line " + e.getLineNumber() + ". Please check for issues like unclosed quotes or incorrect column counts.", e);
        }
        return rows;
    }

    private String cleanExcelValue(String value) {
        if (value == null) return null;
        value = value.trim();
        // Handle ="value" format from CSV
        if (value.matches("^=\".*\"$")) {
            value = value.substring(2, value.length() - 1);
        }
        // Remove surrounding quotes if present (from XLSX)
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Extracts both Invoice Number and Account Number from a PDF's text content.
     * It specifically looks for labels like "Invoice:" and "Account Number:" to improve accuracy.
     *
     * @param inputStream The input stream of the PDF file.
     * @return A Map containing "invoiceNumber" and "accountNumber" if found, otherwise an empty map.
     * @throws IOException if the PDF stream cannot be read.
     */
    public Map<String, String> extractInvoiceAndAccountNumbers(InputStream inputStream) throws IOException {
        Map<String, String> extractedData = new HashMap<>();
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(1); // Invoice/Account info is usually on the first page
            String text = pdfStripper.getText(document);

            // More specific pattern for Invoice to avoid ambiguity
            Pattern invoicePattern = Pattern.compile("Invoice:\\s*([\\w\\-]+)");
            Matcher invoiceMatcher = invoicePattern.matcher(text);
            if (invoiceMatcher.find()) {
                extractedData.put("invoiceNumber", invoiceMatcher.group(1).trim());
            }

            // More specific pattern for Account Number
            Pattern accountPattern = Pattern.compile("Account Number:\\s*(\\d+)");
            Matcher accountMatcher = accountPattern.matcher(text);
            if (accountMatcher.find()) {
                extractedData.put("accountNumber", accountMatcher.group(1).trim());
            }
        }
        return extractedData;
    }

    /**
     * Validates that the headers from an uploaded file match the expected headers for a given provider.
     *
     * @param actualHeaders The list of headers found in the uploaded file.
     * @param provider      The name of the provider to validate against.
     * @throws IllegalArgumentException if the provider is not supported or if required headers are missing.
     */
    private void validateHeaders(List<String> actualHeaders, String provider, FileType fileType) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty for header validation.");
        }

        String configKey = provider;
        if (fileType == FileType.INVENTORY) {
            // For inventory uploads, append "Inventory" to the carrier name.
            configKey = provider + "Inventory";
        }

        ProviderConfig config = providerConfigs.get(configKey.toLowerCase());
        if (config == null) {
            throw new IllegalArgumentException("Unsupported provider: '" + provider + "'. No configuration found.");
        }

        // Use sets for efficient comparison of headers.
        List<String> expectedHeaders = config.getExpectedHeaders();
        Set<String> expectedSet = expectedHeaders.stream().map(String::trim).collect(Collectors.toSet());
        Set<String> actualSet = actualHeaders.stream().map(String::trim).collect(Collectors.toSet());

        Set<String> missingHeaders = expectedSet.stream()
                .filter(header -> !actualSet.contains(header))
                .collect(Collectors.toSet());

        if (!missingHeaders.isEmpty()) {
            // Provide a clear error message listing the missing columns.
            throw new IllegalArgumentException("The uploaded file is missing required columns for provider '"
                    + provider + "': " + String.join(", ", missingHeaders));
        }
    }
}
