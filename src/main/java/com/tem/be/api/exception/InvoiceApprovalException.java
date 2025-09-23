package com.tem.be.api.exception;

public class InvoiceApprovalException extends RuntimeException {
    public InvoiceApprovalException(String message) {
        super(message);
    }

    public InvoiceApprovalException(String message, Throwable cause) {
        super(message, cause);
    }
}
