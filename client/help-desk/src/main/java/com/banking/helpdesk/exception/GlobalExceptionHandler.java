package com.pranit.praya.exception;

import com.pranit.praya.guardrail.exception.GuardrailViolationException;
import com.pranit.praya.ratelimit.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler providing standard RFC 7807 ProblemDetail responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("[RATE-LIMIT] [REJECTED] path: {}, retryAfterSeconds: {}", request.getRequestURI(), ex.retryAfterSeconds());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Try again in " + ex.retryAfterSeconds() + " seconds."
        );
        pd.setTitle("Too Many Requests");
        pd.setType(URI.create("https://api.praya.ai/errors/rate-limit-exceeded"));
        pd.setProperty("retryAfterSeconds", ex.retryAfterSeconds());
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
                .body(pd);
    }

    @ExceptionHandler(GuardrailViolationException.class)
    public ResponseEntity<ProblemDetail> handleGuardrailViolation(GuardrailViolationException ex, HttpServletRequest request) {
        log.warn("[GUARDRAIL] [VIOLATION] path: {}, phase: {}, message: {}",
                request.getRequestURI(), ex.getPhase(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        pd.setTitle("Safety Guardrail Violation");
        pd.setType(URI.create("https://api.praya.ai/errors/guardrail-violation"));
        pd.setProperty("phase", ex.getPhase());
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("[VALIDATION] [FAILED] path: {}, fieldErrorCount: {}", request.getRequestURI(), ex.getBindingResult().getFieldErrorCount());
        final Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for request parameters."
        );
        pd.setTitle("Validation Failed");
        pd.setType(URI.create("https://api.praya.ai/errors/validation-failed"));
        pd.setProperty("errors", errors);
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("[SYSTEM] [UNHANDLED-EXCEPTION] path: {}, error: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred."
        );
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("https://api.praya.ai/errors/internal-error"));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
