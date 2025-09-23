package com.tem.be.api.exception;

public class ProviderConfigLoadException extends RuntimeException {

    public ProviderConfigLoadException(String message) {
        super(message);
    }

    public ProviderConfigLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
