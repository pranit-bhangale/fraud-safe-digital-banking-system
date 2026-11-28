package com.banking.gateway.controller;

import com.banking.gateway.dto.GatewayErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Spring WebMvc Fallback Controller for Resilience4j Circuit Breakers.
 * Returns structured, client-friendly HTTP 503 responses when downstream services fail or trip open.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
@Tag(name = "Resilience4j Fallback Handlers", description = "Gateway circuit breaker fallback endpoints invoked when downstream microservices degrade or trip open")
public class FallbackController {

    @RequestMapping("/user")
    @Operation(summary = "User Service Fallback", description = "Invoked by Gateway circuit breaker when User Service fails or times out.")
    @ApiResponses({
            @ApiResponse(responseCode = "503", description = "User Service temporarily unavailable", content = @Content(schema = @Schema(implementation = GatewayErrorResponse.class)))
    })
    public ResponseEntity<GatewayErrorResponse> userFallback(HttpServletRequest request) {
        log.warn("Circuit Breaker triggered: User Service is currently unavailable. Path: {}",
                request.getRequestURI());
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "User Service is temporarily unavailable. Circuit breaker open or service degraded.",
                request.getRequestURI());
    }

    @RequestMapping("/account")
    @Operation(summary = "Account Service Fallback", description = "Invoked by Gateway circuit breaker when Account Service fails or times out.")
    @ApiResponses({
            @ApiResponse(responseCode = "503", description = "Account Service temporarily unavailable", content = @Content(schema = @Schema(implementation = GatewayErrorResponse.class)))
    })
    public ResponseEntity<GatewayErrorResponse> accountFallback(HttpServletRequest request) {
        log.warn("Circuit Breaker triggered: Account Service is currently unavailable. Path: {}",
                request.getRequestURI());
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Account Service is temporarily unavailable. Circuit breaker open or service degraded.",
                request.getRequestURI());
    }

    @RequestMapping("/transaction")
    @Operation(summary = "Transaction Service Fallback", description = "Invoked by Gateway circuit breaker when Transaction Service fails or times out.")
    @ApiResponses({
            @ApiResponse(responseCode = "503", description = "Transaction Service temporarily unavailable", content = @Content(schema = @Schema(implementation = GatewayErrorResponse.class)))
    })
    public ResponseEntity<GatewayErrorResponse> transactionFallback(HttpServletRequest request) {
        log.warn("Circuit Breaker triggered: Transaction Service is currently unavailable. Path: {}",
                request.getRequestURI());
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Transaction Service is temporarily unavailable. Circuit breaker open or service degraded.",
                request.getRequestURI());
    }

    @RequestMapping("/payment")
    @Operation(summary = "Payment Service Fallback", description = "Invoked by Gateway circuit breaker when Payment Service fails or times out.")
    @ApiResponses({
            @ApiResponse(responseCode = "503", description = "Payment Service temporarily unavailable", content = @Content(schema = @Schema(implementation = GatewayErrorResponse.class)))
    })
    public ResponseEntity<GatewayErrorResponse> paymentFallback(HttpServletRequest request) {
        log.warn("Circuit Breaker triggered: Payment Service is currently unavailable. Path: {}",
                request.getRequestURI());
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Payment Service is temporarily unavailable. Circuit breaker open or service degraded.",
                request.getRequestURI());
    }

    @RequestMapping("/default")
    @Operation(summary = "Default Downstream Fallback", description = "Invoked by Gateway circuit breaker for unspecified downstream failures.")
    @ApiResponses({
            @ApiResponse(responseCode = "503", description = "Downstream service temporarily unavailable", content = @Content(schema = @Schema(implementation = GatewayErrorResponse.class)))
    })
    public ResponseEntity<GatewayErrorResponse> defaultFallback(HttpServletRequest request) {
        log.warn("Circuit Breaker triggered: Downstream service is currently unavailable. Path: {}",
                request.getRequestURI());
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "The requested downstream banking service is temporarily unavailable. Please retry shortly.",
                request.getRequestURI());
    }

    private ResponseEntity<GatewayErrorResponse> buildFallbackResponse(HttpStatus status, String message, String path) {
        GatewayErrorResponse response = GatewayErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
