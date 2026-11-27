package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for creating and inspecting banking transactions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> initiateTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("Received transaction initiation request: sender: {} amount: {}", request.senderAccountNo(), request.amount());
        TransactionResponse response = this.transactionService.createNewTransaction(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{referenceNo}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransactionByReference(@PathVariable("referenceNo") String referenceNo) {
        return ResponseEntity.ok(this.transactionService.getTransactionByReference(referenceNo));
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(this.transactionService.getTransactionById(id));
    }
}
