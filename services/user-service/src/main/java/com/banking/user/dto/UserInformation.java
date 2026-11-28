package com.banking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Internal user demographic summary for inter-service communication")
public record UserInformation(
        @Schema(description = "User unique UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        String lastName,

        @Schema(description = "Email address", example = "john.doe@example.com")
        String email
) {
}
