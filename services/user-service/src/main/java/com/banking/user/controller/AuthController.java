package com.banking.user.controller;

import com.banking.user.dto.AuthenticatedUser;
import com.banking.user.dto.LoginRequest;
import com.banking.user.dto.LoginResponse;
import com.banking.user.dto.RegistrationRequest;
import com.banking.user.exception.InvalidRequestException;
import com.banking.user.security.service.ResolveAccessToken;
import com.banking.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication & User Management", description = "Endpoints for customer registration, authentication, JWT token lifecycle, and profiles")
public class AuthController {

    private final AuthService authService;
    private final ResolveAccessToken resolveAccessToken;

    @PostMapping(value = "/register", version = "v1")
    @SecurityRequirements
    @Operation(summary = "Register New Customer", description = "Creates a new customer account with role assignment and triggers welcome notification.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or password mismatch", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content)
    })
    public ResponseEntity<String> registerNewUser(@Valid @RequestBody RegistrationRequest request) {
        this.authService.createNewAccount(request);
        return ResponseEntity.created(URI.create("/api/auth/login")).body("Account Created Successfully.");
    }

    @PostMapping(value = "/login", version = "v1")
    @SecurityRequirements
    @Operation(summary = "Authenticate User", description = "Authenticates user credentials and issues an RSA-signed JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(this.authService.authenticateUser(request));
    }

    @GetMapping(value = "/me", version = "v1")
    @Operation(summary = "Get Current User Profile", description = "Returns identity and roles of the currently authenticated user from Bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved", content = @Content(schema = @Schema(implementation = AuthenticatedUser.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Missing or invalid JWT", content = @Content)
    })
    public ResponseEntity<AuthenticatedUser> currentUser() {
        return ResponseEntity.ok(this.authService.getCurrentUser());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout & Revoke Token", description = "Revokes the active JWT access token by adding it to the token revocation blacklist.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successfully logged out"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid authorization header", content = @Content)
    })
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        final String token = this.resolveAccessToken.getAccessTokenFromRequest(request);
        if (token == null) {
            throw new InvalidRequestException("Authorization header is missing or invalid");
        }
        this.authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
