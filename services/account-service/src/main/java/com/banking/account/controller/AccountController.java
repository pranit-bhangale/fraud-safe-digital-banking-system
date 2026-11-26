package com.banking.account.controller;

import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
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

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @PostMapping(version = "v1")
    public ResponseEntity<String> createNewAccount(@Valid @RequestBody AccountCreationRequest request) {
        this.accountService.createNewAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Account Created Successfully.");
    }

    @GetMapping(value = "/{accountNo}", version = "v1")
    public ResponseEntity<AccountDetail> getAccountDetail(
            @PathVariable String accountNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(this.accountService.fetchAccountDetail(accountNo, page, size));
    }
}
