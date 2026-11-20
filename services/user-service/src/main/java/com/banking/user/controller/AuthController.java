package com.banking.user.controller;

import com.banking.user.dto.AuthenticatedUser;
import com.banking.user.dto.LoginRequest;
import com.banking.user.dto.LoginResponse;
import com.banking.user.dto.RegistrationRequest;
import com.banking.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/register", version = "v1")
    public ResponseEntity<String> registerNewUser(@Valid @RequestBody RegistrationRequest request) {
        this.authService.createNewAccount(request);
        return ResponseEntity.created(URI.create("/api/auth/login")).body("Account Created Successfully.");
    }

    @PostMapping(value = "/login", version = "v1")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(this.authService.authenticateUser(request));
    }

    @GetMapping(value = "/me", version = "v1")
    public ResponseEntity<AuthenticatedUser> currentUser() {
        return ResponseEntity.ok(this.authService.getCurrentUser());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) final String authorization) {
        this.authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
