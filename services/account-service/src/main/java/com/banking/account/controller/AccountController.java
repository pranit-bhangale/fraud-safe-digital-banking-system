package com.banking.account.controller;

import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import com.banking.account.dto.BalanceOperationRequest;
import com.banking.account.dto.BalanceOperationResponse;
import com.banking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
@Tag(name = "Account Management", description = "Endpoints for creating customer bank accounts, querying ledgers, and executing Saga balance locks/adjustments")
public class AccountController {

    private final AccountService accountService;

    @PostMapping(version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create New Bank Account", description = "Creates a new CHECKING, SAVINGS, or CREDIT bank account for a validated customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(example = "{\"success\":true,\"message\":\"Account Created Successfully.\",\"accountNo\":\"ACC-1234567890\"}"))),
            @ApiResponse(responseCode = "400", description = "Initial balance below minimum required or invalid type", content = @Content),
            @ApiResponse(responseCode = "404", description = "User ID does not exist in User Service", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> createNewAccount(@Valid @RequestBody AccountCreationRequest request) {
        String accountNo = this.accountService.createNewAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Account Created Successfully.",
                "accountNo", accountNo
        ));
    }

    @GetMapping(value = "/{accountNo}", version = "v1")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Get Account Details & Ledger", description = "Fetches account balance, status, owner demographic info, and paginated transaction ledger.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account details found", content = @Content(schema = @Schema(implementation = AccountDetail.class))),
            @ApiResponse(responseCode = "404", description = "Account number not found", content = @Content)
    })
    public ResponseEntity<AccountDetail> getAccountDetail(
            @Parameter(description = "Account number (e.g. ACC-1234567890)", required = true)
            @PathVariable String accountNo,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(this.accountService.fetchAccountDetail(accountNo, page, size));
    }

    // Saga balance operations (restricted high-privilege internal balance operations)
    @PostMapping(value = "/{accountNo}/balance/reserve", version = "v1")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @internalSecurityEvaluator.isInternalRequest()")
    @Operation(summary = "Reserve Balance (Saga Step)", description = "Locks funds from available balance into reserved status during distributed transaction processing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance reserved", content = @Content(schema = @Schema(implementation = BalanceOperationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Insufficient funds", content = @Content)
    })
    public ResponseEntity<BalanceOperationResponse> reserveBalance(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.reserveBalance(accountNo, request));
    }

    @PostMapping(value = "/{accountNo}/balance/release", version = "v1")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @internalSecurityEvaluator.isInternalRequest()")
    @Operation(summary = "Release Reservation (Saga Rollback)", description = "Releases reserved funds back into available balance when a transaction is aborted or compensated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservation released", content = @Content(schema = @Schema(implementation = BalanceOperationResponse.class)))
    })
    public ResponseEntity<BalanceOperationResponse> releaseReservation(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.releaseReservation(accountNo, request));
    }

    @PostMapping(value = "/{accountNo}/balance/debit", version = "v1")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @internalSecurityEvaluator.isInternalRequest()")
    @Operation(summary = "Debit Balance (Saga Forward)", description = "Deducts settled transaction amount from sender account ledger.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account debited", content = @Content(schema = @Schema(implementation = BalanceOperationResponse.class)))
    })
    public ResponseEntity<BalanceOperationResponse> debitBalance(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.debitBalance(accountNo, request));
    }

    @PostMapping(value = "/{accountNo}/balance/credit", version = "v1")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @internalSecurityEvaluator.isInternalRequest()")
    @Operation(summary = "Credit Balance (Saga Final / Refund)", description = "Credits incoming transaction amount to receiver account ledger.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account credited", content = @Content(schema = @Schema(implementation = BalanceOperationResponse.class)))
    })
    public ResponseEntity<BalanceOperationResponse> creditBalance(
            @Parameter(description = "Account number", required = true)
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.creditBalance(accountNo, request));
    }
}
