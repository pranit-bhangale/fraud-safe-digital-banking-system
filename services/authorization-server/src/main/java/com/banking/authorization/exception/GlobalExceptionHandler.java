package com.banking.authorization.exception;

import com.banking.utility.wrapper.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            final BadCredentialsException ex, final HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password.", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            final HttpStatus status, final String message, final HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status.value())
                        .message(message)
                        .path(request.getRequestURI())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            final DisabledException ex, final HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            final UsernameNotFoundException ex, final HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            final MethodArgumentNotValidException ex, final HttpServletRequest request) {
        final Map<String, String> errors = new LinkedHashMap<>();
        for (final FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Validation failed")
                        .errors(errors)
                        .path(request.getRequestURI())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            final IllegalStateException ex, final HttpServletRequest request) {
        log.error("Service unavailable or state error: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            final Exception ex, final HttpServletRequest request) {
        log.error("Unhandled exception occurred in Authorization Server", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }
}
