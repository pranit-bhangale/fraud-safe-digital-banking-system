package com.banking.transaction.client;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data transfer object representing account details received from account-service.
 */
@Builder
public record AccountDto(
        UUID id,
        String accountNo,
        String type,
        String status,
        BigDecimal balance
) {
}
