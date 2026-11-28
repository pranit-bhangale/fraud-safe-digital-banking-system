package com.banking.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Schema(description = "Balance mutation request for distributed Saga orchestration")
public record BalanceOperationRequest(
        @Schema(description = "Associated transaction UUID", example = "660e8400-e29b-41d4-a716-446655440001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Transaction ID is required")
        UUID transactionId,

        @Schema(description = "Associated Saga orchestrator UUID", example = "770e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Saga ID is required")
        UUID sagaId,

        @Schema(description = "Amount to reserve, release, debit, or credit", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,

        @Schema(description = "Audit trail operation description", example = "Reserve for pending transaction")
        String description
) {
}
