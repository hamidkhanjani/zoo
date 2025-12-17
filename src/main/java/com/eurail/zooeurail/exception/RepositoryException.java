package com.eurail.zooeurail.exception;

/**
 * Exception thrown when a repository layer operation fails.
 * This exception is typically used for scenarios where database operations,
 * data access errors, or persistence layer issues occur.
 * <p>
 * This exception extends {@link AppException} and represents errors that
 * originate from the repository layer of the application.
 * </p>
 *
 * @see AppException
 * @see com.eurail.zooeurail.repository.BaseRepository
 * @see com.eurail.zooeurail.repository.AnimalRepository
 */
public class RepositoryException extends AppException {
    /**
     * Constructs a new RepositoryException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the repository error
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Constructs a new RepositoryException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the repository error
     * @param cause   the cause of the exception (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}