package com.banking.utility.wrapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error response payload for REST controllers and exception handlers.
 *
 * @param status    the HTTP status code
 * @param message   the high-level error message or description
 * @param errors    optional field-level validation errors or details
 * @param path      the request URI path where the error occurred
 * @param timestamp the timestamp when the error occurred
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        String path,
        Instant timestamp
) {
}
