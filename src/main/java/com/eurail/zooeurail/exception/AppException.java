package com.eurail.zooeurail.exception;

/**
 * Base application exception that extends RuntimeException.
 * This exception serves as the parent class for all custom application exceptions
 * in the Zoo application.
 * <p>
 * Subclasses include:
 * <ul>
 *   <li>{@link NotFoundException} - for resource not found scenarios</li>
 *   <li>{@link ValidationException} - for validation failures</li>
 *   <li>{@link ConflictException} - for conflict scenarios</li>
 *   <li>{@link ServiceException} - for service layer errors</li>
 *   <li>{@link RepositoryException} - for repository layer errors</li>
 * </ul>
 * </p>
 */
public class AppException extends RuntimeException {
    /**
     * Constructs a new AppException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public AppException(String message) {
        super(message);
    }

    /**
     * Constructs a new AppException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the cause of the exception (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}