package com.eurail.zooeurail.exception;

/**
 * Exception thrown when an error occurs in the service layer.
 * This exception is typically used for business logic errors, service-level failures,
 * or other issues that occur during service operations.
 * <p>
 * This exception extends {@link AppException} and is handled by the global exception handler.
 * </p>
 *
 * @see AppException
 * @see com.eurail.zooeurail.exception.GlobalExceptionHandler
 */
public class ServiceException extends AppException {
    /**
     * Constructs a new ServiceException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new ServiceException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the cause of the exception (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}