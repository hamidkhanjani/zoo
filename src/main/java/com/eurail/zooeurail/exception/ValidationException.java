package com.eurail.zooeurail.exception;

/**
 * Exception thrown when validation fails in the application.
 * This exception is typically used for input validation errors, constraint violations,
 * or other validation-related failures that occur when processing requests.
 * <p>
 * This exception extends {@link AppException} and is mapped to HTTP 400 (Bad Request)
 * status code by the global exception handler.
 * </p>
 *
 * @see AppException
 * @see com.eurail.zooeurail.exception.GlobalExceptionHandler
 */
public class ValidationException extends AppException {
    /**
     * Constructs a new ValidationException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }
}