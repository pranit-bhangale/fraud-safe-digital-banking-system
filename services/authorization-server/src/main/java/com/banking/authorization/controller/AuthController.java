package com.banking.authorization.controller;

import com.banking.authorization.config.RsaKeyProvider;
import com.banking.authorization.dto.AuthResponse;
import com.banking.authorization.dto.LoginRequest;
import com.banking.authorization.dto.RefreshTokenRequest;
import com.banking.authorization.dto.TokenValidationResponse;
import com.banking.authorization.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final RsaKeyProvider rsaKeyProvider;

    @PostMapping(value = "/login", version = "v1")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody final LoginRequest request) {
        log.info("REST login request received for user: {}", request.username());
        final AuthResponse response = this.authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/refresh", version = "v1")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody final RefreshTokenRequest request) {
        log.info("REST token refresh request received");
        final AuthResponse response = this.authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/validate", version = "v1")
    public ResponseEntity<TokenValidationResponse> validate(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) final String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(TokenValidationResponse.builder()
                            .valid(false)
                            .message("Missing Authorization header")
                            .build());
        }
        final TokenValidationResponse response = this.authService.validateToken(authHeader);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/public-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getPublicKey() {
        return ResponseEntity.ok(this.rsaKeyProvider.getPublicKeyPem());
    }
}
