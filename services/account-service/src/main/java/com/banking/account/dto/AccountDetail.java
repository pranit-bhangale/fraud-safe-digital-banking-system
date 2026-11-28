package com.banking.account.dto;

import com.banking.account.model.AccountStatus;
import com.banking.account.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Schema(description = "Complete account summary including demographics, balances, and paginated ledger")
public record AccountDetail(
        @Valid
        @Schema(description = "Demographic information of the account owner")
        UserInformation user,

        @Valid
        @Schema(description = "Financial status and ledger information of the account")
        AccountInformation account
) {

    @Builder
    @Schema(description = "Customer demographic summary")
    public record UserInformation(
            @Schema(description = "Customer unique UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID userId,

            @Schema(description = "First name", example = "John")
            String firstName,

            @Schema(description = "Last name", example = "Doe")
            String lastName,

            @Schema(description = "Email address", example = "john.doe@example.com")
            String email
    ) {
    }

    @Builder
    @Schema(description = "Account balance and ledger summary")
    public record AccountInformation(
            @Schema(description = "Internal account UUID", example = "a10e8400-e29b-41d4-a716-446655440001")
            UUID id,

            @Schema(description = "Public banking account number", example = "ACC-1234567890")
            String accountNo,

            @Schema(description = "Account type", example = "CHECKING")
            AccountType type,

            @Schema(description = "Account operational status", example = "ACTIVE")
            AccountStatus status,

            @Schema(description = "Current account balance", example = "1000.00")
            BigDecimal balance,

            @Schema(description = "Paginated transaction history")
            PaginatedTransactionHistory history
    ) {
    }

    @Builder
    @Schema(description = "Individual transaction ledger entry")
    public record TransactionHistory(
            @Schema(description = "Transaction UUID", example = "660e8400-e29b-41d4-a716-446655440001")
            UUID transactionId,

            @Schema(description = "Transaction reference number", example = "TXN-abc123def456")
            String referenceNo,

            @Schema(description = "Transaction type (e.g. DEPOSIT, TRANSFER)", example = "TRANSFER")
            String type,

            @Schema(description = "Transaction amount", example = "250.00")
            BigDecimal amount,

            @Schema(description = "Account balance following transaction completion", example = "750.00")
            BigDecimal balanceAfter,

            @Schema(description = "Transaction memo", example = "Monthly rent payment")
            String description,

            @Schema(description = "Timestamp when transaction was recorded")
            Instant transactionDate
    ) {
    }

    @Builder
    @Schema(description = "Paginated transaction ledger response")
    public record PaginatedTransactionHistory(
            List<TransactionHistory> transactions,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
