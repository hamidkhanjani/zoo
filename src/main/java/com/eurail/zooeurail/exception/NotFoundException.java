package com.eurail.zooeurail.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 * This exception is typically used for scenarios where a resource (such as an entity
 * identified by an ID) does not exist in the system.
 * <p>
 * This exception extends {@link AppException} and is mapped to HTTP 404 (Not Found)
 * status code by the global exception handler.
 * </p>
 *
 * @see AppException
 * @see com.eurail.zooeurail.exception.GlobalExceptionHandler
 */
public class NotFoundException extends AppException {
    /**
     * Constructs a new NotFoundException with the specified detail message.
     *
     * @param message the detail message explaining what resource was not found
     */
    public NotFoundException(String message) {
        super(message);
    }
}