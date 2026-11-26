
package com.banking.account.dto;

import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import jakarta.validation.Valid;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record AccountDetail(
        @Valid
        UserInformation user,
        @Valid
        AccountInformation account

) {

    @Builder
    public record UserInformation(
            UUID userId,
            String firstName,
            String lastName,
            String email
    ) {
    }

    @Builder
    public record AccountInformation(
            UUID id,
            String accountNo,
            AccountType type,
            AccountStatus status,
            BigDecimal balance,
            PaginatedTransactionHistory history
    ) {
    }

    @Builder
    public record TransactionHistory(
            UUID transactionId,
            String referenceNo,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            Instant transactionDate
    ) {
    }

    @Builder
    public record PaginatedTransactionHistory(
            List<TransactionHistory> transactions,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static PaginatedTransactionHistory empty(int page, int size) {
            return new PaginatedTransactionHistory(List.of(), page, size, 0L, 0, false, false);
        }
    }
}
