package com.eurail.zooeurail.exception;

/**
 * Exception thrown when a conflict occurs in the application.
 * This exception is typically used for scenarios where a resource conflict is detected,
 * such as duplicate entries, version conflicts, or state conflicts that prevent
 * an operation from completing successfully.
 * <p>
 * This exception extends {@link AppException} and is mapped to HTTP 409 (Conflict)
 * status code by the global exception handler.
 * </p>
 *
 * @see AppException
 * @see com.eurail.zooeurail.exception.GlobalExceptionHandler#handleConflict(ConflictException, jakarta.servlet.http.HttpServletRequest)
 */
public class ConflictException extends AppException {
    /**
     * Constructs a new ConflictException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the conflict
     */
    public ConflictException(String message) {
        super(message);
    }
}