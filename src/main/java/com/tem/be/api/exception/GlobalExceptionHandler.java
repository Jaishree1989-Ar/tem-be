package com.tem.be.api.exception;

import com.tem.be.api.utils.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler to catch and handle exceptions across the entire application.
 */
@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all uncaught {@link RuntimeException}s in the application.
     *
     * @param e The thrown RuntimeException.
     * @return A response entity with status 500 and the exception message.
     */
    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<Object> handleRuntimeException(RuntimeException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    /**
     * Handles {@link ResourceNotFoundException} when a requested resource is not found.
     *
     * @param e The thrown ResourceNotFoundException.
     * @return A response entity with status 500 and the exception message.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    /**
     * Handles {@link ResourceAlreadyExistsException} when attempting to create a resource that already exists.
     *
     * @param e The thrown ResourceAlreadyExistsException.
     * @return A response entity with status 500 and the exception message.
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Object> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.error(ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.CONFLICT.value(), ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}
