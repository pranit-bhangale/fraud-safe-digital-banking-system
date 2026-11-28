package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for creating and inspecting banking transactions.
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction & Saga Management", description = "Endpoints for initiating financial transactions, monitoring Saga state, and querying transaction details")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping(version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Initiate Transaction", description = "Initiates a new financial transaction (TRANSFER, DEPOSIT, WITHDRAWAL) and dispatches it to the distributed Saga orchestrator.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Transaction accepted for processing", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<TransactionResponse> initiateTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("Received transaction initiation request: sender: {} amount: {}", request.senderAccountNo(), request.amount());
        TransactionResponse response = this.transactionService.createNewTransaction(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping(value = "/{referenceNo}", version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Transaction by Reference", description = "Queries transaction status, fraud score, and settlement details by reference number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction reference not found", content = @Content)
    })
    public ResponseEntity<TransactionResponse> getTransactionByReference(
            @Parameter(description = "Transaction reference number (e.g. TXN-abc123def456)", required = true)
            @PathVariable("referenceNo") String referenceNo) {
        return ResponseEntity.ok(this.transactionService.getTransactionByReference(referenceNo));
    }

    @GetMapping(value = "/id/{id}", version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Transaction by UUID", description = "Queries transaction status, fraud score, and settlement details by internal UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction ID not found", content = @Content)
    })
    public ResponseEntity<TransactionResponse> getTransactionById(
            @Parameter(description = "Transaction internal UUID", required = true)
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(this.transactionService.getTransactionById(id));
    }
}
