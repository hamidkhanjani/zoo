package com.eurail.zooeurail.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * <p>
 * This class provides centralized exception handling across all {@code @RestController} classes
 * using Spring's {@code @RestControllerAdvice}. It intercepts various application-specific and
 * generic exceptions, logging them appropriately and returning standardized error responses.
 * </p>
 *
 * @see ErrorResponse
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link NotFoundException} exceptions.
     * <p>
     * Returns a 404 NOT FOUND response when a requested resource cannot be found.
     * </p>
     *
     * @param ex  the NotFoundException that was thrown
     * @param req the HTTP servlet request
     * @return a ResponseEntity containing the error response with 404 status
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex, req, false);
    }

    /**
     * Handles validation exceptions including {@link ValidationException} and
     * {@link MethodArgumentNotValidException}.
     * <p>
     * Returns a 400 BAD REQUEST response. For {@code MethodArgumentNotValidException},
     * field validation errors are collected and formatted into a readable message.
     * </p>
     *
     * @param ex  the validation exception that was thrown
     * @param req the HTTP servlet request
     * @return a ResponseEntity containing the error response with 400 status
     */
    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        String message = ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException manve) {
            message = manve.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }
        return build(HttpStatus.BAD_REQUEST, message, ex, req, false);
    }

    /**
     * Handles {@link ConflictException} exceptions.
     * <p>
     * Returns a 409 CONFLICT response when a request conflicts with the current state
     * of the resource.
     * </p>
     *
     * @param ex  the ConflictException that was thrown
     * @param req the HTTP servlet request
     * @return a ResponseEntity containing the error response with 409 status
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex, req, false);
    }

    /**
     * Handles {@link ServiceException} exceptions.
     * <p>
     * Returns a 500 INTERNAL SERVER ERROR response when an error occurs in the service layer.
     * These errors are logged at ERROR level with full stack traces.
     * </p>
     *
     * @param ex  the ServiceException that was thrown
     * @param req the HTTP servlet request
     * @return a ResponseEntity containing the error response with 500 status
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleService(ServiceException ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, req, true);
    }

    /**
     * Handles {@link RepositoryException} exceptions.
     * <p>
     * Returns a 500 INTERNAL SERVER ERROR response when an error occurs in the repository layer.
     * These errors are logged at ERROR level with full stack traces.
     * </p>
     *
     * @param ex  the RepositoryException that was thrown
     * @param req the HTTP servlet request
     * @return a ResponseEntity containing the error response with 500 status
     */
    @ExceptionHandler(RepositoryException.class)
    public ResponseEntity<ErrorResponse> handleRepo(RepositoryException ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, req, true);
    }

    /**
     * Handles all other unhandled exceptions.
     * <p>
     * Returns a 500 INTERNAL SERVER ERROR response for any exception not explicitly handled
     * by other exception handlers. These errors are logged at ERROR level with full stack traces.
     * </p>
     *
     * @param ex  the generic exception that was thrown
     * @param req the HTTP servlet request
     * @return a ResponseEntity containing the error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, req, true);
    }

    /**
     * Builds an error response with the exception's message.
     *
     * @param status     the HTTP status to return
     * @param ex         the exception that was thrown
     * @param req        the HTTP servlet request
     * @param errorLevel whether to log at ERROR level (true) or WARN level (false)
     * @return a ResponseEntity containing the error response
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, Throwable ex, HttpServletRequest req, boolean errorLevel) {
        return build(status, ex.getMessage(), ex, req, errorLevel);
    }

    /**
     * Builds an error response with a custom message.
     * <p>
     * Constructs an {@link ErrorResponse} object and logs the error appropriately based on
     * the error level flag. ERROR level includes stack traces while WARN level does not.
     * </p>
     *
     * @param status     the HTTP status to return
     * @param message    the error message to include in the response
     * @param ex         the exception that was thrown
     * @param req        the HTTP servlet request
     * @param errorLevel whether to log at ERROR level (true) or WARN level (false)
     * @return a ResponseEntity containing the error response
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Throwable ex, HttpServletRequest req, boolean errorLevel) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        if (errorLevel) {
            log.error("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), message, ex);
        } else {
            log.warn("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), status.value(), message);
        }
        return ResponseEntity.status(status).body(body);
    }
}