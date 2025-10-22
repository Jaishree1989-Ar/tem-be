package com.tem.be.api.exception;

public class PdfParseException extends Exception {
    public PdfParseException(String message) {
        super(message);
    }

    public PdfParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
