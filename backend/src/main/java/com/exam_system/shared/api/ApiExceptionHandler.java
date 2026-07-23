package com.exam_system.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException exception,
                                                               HttpServletRequest request) {
        logWarn(request, HttpStatus.NOT_FOUND, exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception,
                                                                HttpServletRequest request) {
        FieldError firstError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError == null ? "Validation error" : firstError.getDefaultMessage();
        logWarn(request, HttpStatus.BAD_REQUEST, exception, message);

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "VALIDATION_ERROR",
                        "message", message
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception,
                                                                 HttpServletRequest request) {
        logWarn(request, HttpStatus.BAD_REQUEST, exception);
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", exception.getMessage()
                ));
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException exception,
                                                               HttpServletRequest request) {
        logWarn(request, HttpStatus.CONFLICT, exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "CONFLICT",
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException exception,
                                                                     HttpServletRequest request) {
        logWarn(request, HttpStatus.UNAUTHORIZED, exception, "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "UNAUTHORIZED",
                        "message", "Invalid credentials"
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception,
                                                                   HttpServletRequest request) {
        logWarn(request, HttpStatus.FORBIDDEN, exception, "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "FORBIDDEN",
                        "message", "Access denied"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception,
                                                                HttpServletRequest request) {
        logError(request, HttpStatus.INTERNAL_SERVER_ERROR, exception, "Unexpected error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_SERVER_ERROR",
                        "message", "Unexpected error"
                ));
    }

    private void logWarn(HttpServletRequest request, HttpStatus status, Exception exception) {
        logWarn(request, status, exception, exception.getMessage());
    }

    private void logWarn(HttpServletRequest request, HttpStatus status, Exception exception, String message) {
        logger.warn(
                "request-error method={} path={} status={} error={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                exception.getClass().getSimpleName(),
                message
        );
    }

    private void logError(HttpServletRequest request, HttpStatus status, Exception exception, String message) {
        logger.error(
                "request-error method={} path={} status={} error={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                exception.getClass().getSimpleName(),
                message,
                exception
        );
    }
}
