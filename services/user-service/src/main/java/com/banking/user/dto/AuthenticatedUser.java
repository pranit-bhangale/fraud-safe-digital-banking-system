package com.banking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Collection;
import java.util.UUID;

@Builder
@Schema(description = "Authenticated user profile details")
public record AuthenticatedUser(
        @Schema(description = "User unique UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Username", example = "johndoe")
        String username,

        @Schema(description = "Email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "Assigned security roles", example = "[\"ROLE_CUSTOMER\"]")
        Collection<String> roles
) {
}
