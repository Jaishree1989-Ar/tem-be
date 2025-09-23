package com.tem.be.api.exception;

public class EmailAuthenticationException extends RuntimeException {
    public EmailAuthenticationException(String message) {
        super(message);
    }

  public EmailAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
