package com.banking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Authentication response with RSA JWT access token")
public record LoginResponse(
        @Schema(description = "Authenticated username", example = "johndoe")
        String username,

        @Schema(description = "RSA-256 signed JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Token validity duration in seconds", example = "900")
        long expiresIn
) {
}
