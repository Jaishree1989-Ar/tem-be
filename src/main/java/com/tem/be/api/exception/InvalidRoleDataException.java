package com.tem.be.api.exception;

public class InvalidRoleDataException extends RuntimeException {
    public InvalidRoleDataException(String message) {
        super(message);
    }

    public InvalidRoleDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
