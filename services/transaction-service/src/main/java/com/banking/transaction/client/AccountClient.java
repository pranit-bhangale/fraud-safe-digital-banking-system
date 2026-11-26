package com.banking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for declarative inter-service communication with account-service.
 */
@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8082}")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{accountNo}")
    AccountDto getAccount(@PathVariable("accountNo") String accountNo);
}
