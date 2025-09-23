package com.tem.be.api.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Custom Jackson deserializer to handle multiple date formats.
 * It will attempt to parse a date string using a predefined list of formats.
 */
public class MultiDateDeserializer extends JsonDeserializer<Date> {

    // Add all the date formats you want to support here
    private static final String[] SUPPORTED_FORMATS = {
            "MM-dd-yyyy",
            "MM/dd/yyyy",
            "MM/dd/yy",
            "yyyy-MM-dd"
    };

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        for (String format : SUPPORTED_FORMATS) {
            try {
                return new SimpleDateFormat(format).parse(dateString);
            } catch (ParseException e) {
                // Ignore and try the next format
            }
        }

        // If no format matches, throw an exception
        throw new IOException("Unable to parse date: \"" + dateString + "\". Supported formats are: " + Arrays.toString(SUPPORTED_FORMATS));
    }
}
