package com.banking.account.controller;

import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import com.banking.account.dto.BalanceOperationRequest;
import com.banking.account.dto.BalanceOperationResponse;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class AccountController {

    private final AccountService accountService;

    @PostMapping(version = "v1")
    public ResponseEntity<Map<String, Object>> createNewAccount(@Valid @RequestBody AccountCreationRequest request) {
        String accountNo = this.accountService.createNewAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Account Created Successfully.",
                "accountNo", accountNo
        ));
    }

    @GetMapping(value = "/{accountNo}", version = "v1")
    public ResponseEntity<AccountDetail> getAccountDetail(
            @PathVariable String accountNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(this.accountService.fetchAccountDetail(accountNo, page, size));
    }

    // Saga balance operations (internal APIs for transaction-service)
    @PostMapping(value = "/{accountNo}/balance/reserve", version = "v1")
    public ResponseEntity<BalanceOperationResponse> reserveBalance(
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.reserveBalance(accountNo, request));
    }

    @PostMapping(value = "/{accountNo}/balance/release", version = "v1")
    public ResponseEntity<BalanceOperationResponse> releaseReservation(
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.releaseReservation(accountNo, request));
    }

    @PostMapping(value = "/{accountNo}/balance/debit", version = "v1")
    public ResponseEntity<BalanceOperationResponse> debitBalance(
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.debitBalance(accountNo, request));
    }

    @PostMapping(value = "/{accountNo}/balance/credit", version = "v1")
    public ResponseEntity<BalanceOperationResponse> creditBalance(
            @PathVariable String accountNo,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(this.accountService.creditBalance(accountNo, request));
    }
}
