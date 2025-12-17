package com.eurail.zooeurail.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents the standard error response structure for API exceptions.
 * This class encapsulates error information including timestamp, HTTP status,
 * error type, message, and the request path where the error occurred.
 * <p>
 * Used by {@link GlobalExceptionHandler} to format error responses consistently
 * across the application.
 * </p>
 *
 * @see GlobalExceptionHandler
 */
@Setter
@Getter
public class ErrorResponse {
    /**
     * The timestamp when the error occurred, formatted as a string in ISO-8601 format.
     * Defaults to the current date and time when the ErrorResponse is instantiated.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime timestamp = OffsetDateTime.now();

    /**
     * The HTTP status code of the error response (e.g., 404, 500).
     */
    private int status;

    /**
     * The HTTP status reason phrase (e.g., "Not Found", "Internal Server Error").
     */
    private String error;

    /**
     * A detailed message describing the error.
     */
    private String message;

    /**
     * The request URI path where the error occurred.
     */
    private String path;

    /**
     * Default constructor that creates an ErrorResponse with default timestamp.
     */
    public ErrorResponse() {
    }

    /**
     * Constructs an ErrorResponse with specified error details.
     *
     * @param status  the HTTP status code
     * @param error   the HTTP status reason phrase
     * @param message the detailed error message
     * @param path    the request URI path where the error occurred
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

}