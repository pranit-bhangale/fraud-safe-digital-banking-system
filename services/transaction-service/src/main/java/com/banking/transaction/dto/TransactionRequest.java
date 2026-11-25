package com.banking.transaction.dto;

import java.math.BigDecimal;

public record TransactionRequest(
        String accountNo,
        BigDecimal amount,
        String description
) {
}
