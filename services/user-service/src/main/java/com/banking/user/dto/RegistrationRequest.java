package com.banking.user.dto;

import com.banking.user.model.RoleType;
import com.banking.utility.annotation.PasswordMatches;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@PasswordMatches
@Schema(description = "Customer registration request payload")
public record RegistrationRequest(

        @Schema(description = "Customer first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @Schema(description = "Customer last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @Schema(description = "Desired unique username", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(description = "Customer primary email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @Schema(description = "Account password (min 8 characters)", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Schema(description = "Password confirmation match", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @Schema(description = "Account role type", example = "CUSTOMER", defaultValue = "CUSTOMER")
        RoleType role
) {
    public RegistrationRequest {
        if (role == null) {
            role = RoleType.CUSTOMER;
        }
    }
}
