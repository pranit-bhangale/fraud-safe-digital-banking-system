package com.banking.transaction.client;

import com.banking.transaction.dto.BalanceOperationRequest;
import com.banking.transaction.dto.BalanceOperationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for declarative inter-service communication with account-service.
 */
@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8082}")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{accountNo}")
    AccountDto getAccount(@PathVariable("accountNo") String accountNo);

    @PostMapping("/api/v1/accounts/{accountNo}/balance/reserve")
    BalanceOperationResponse reserveBalance(
            @PathVariable("accountNo") String accountNo,
            @RequestBody BalanceOperationRequest request
    );

    @PostMapping("/api/v1/accounts/{accountNo}/balance/release")
    BalanceOperationResponse releaseReservation(
            @PathVariable("accountNo") String accountNo,
            @RequestBody BalanceOperationRequest request
    );

    @PostMapping("/api/v1/accounts/{accountNo}/balance/debit")
    BalanceOperationResponse debitBalance(
            @PathVariable("accountNo") String accountNo,
            @RequestBody BalanceOperationRequest request
    );

    @PostMapping("/api/v1/accounts/{accountNo}/balance/credit")
    BalanceOperationResponse creditBalance(
            @PathVariable("accountNo") String accountNo,
            @RequestBody BalanceOperationRequest request
    );
}
