package com.banking.authorization.service.impl;

import com.banking.authorization.client.UserClient;
import com.banking.authorization.dto.AuthResponse;
import com.banking.authorization.dto.LoginRequest;
import com.banking.authorization.dto.TokenValidationResponse;
import com.banking.authorization.dto.UserAuthResponse;
import com.banking.authorization.service.AuthService;
import com.banking.authorization.service.JwtTokenService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserClient userClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    public AuthResponse login(final LoginRequest request) {
        log.info("Processing login request for username: {}", request.username());

        final UserAuthResponse user = fetchUserFromClient(request.username());

        if (user == null) {
            log.error("Downstream unavailable (fallback triggered) or user not found for user: {}", request.username());
            throw new IllegalStateException("User service is currently unavailable. Please try again later.");
        }

        if (!user.enabled()) {
            log.warn("Login failed: User '{}' account is disabled", request.username());
            throw new DisabledException("Account is disabled. Please contact support.");
        }

        if (!this.passwordEncoder.matches(request.password(), user.password())) {
            log.warn("Login failed: Password mismatch for user '{}'", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        log.info("Authentication successful for user: {}. Generating Access & Refresh tokens.", request.username());
        return this.jwtTokenService.generateTokens(user);
    }

    @Override
    public AuthResponse refreshToken(final String refreshToken) {
        log.info("Processing token refresh request");
        final String username = this.jwtTokenService.extractUsernameFromRefreshToken(refreshToken);
        final UserAuthResponse user = fetchUserFromClient(username);
        if (user == null) {
            log.error("User service unavailable during token refresh for user: {}", username);
            throw new IllegalStateException("User service is currently unavailable. Please try again later.");
        }
        if (!user.enabled()) {
            log.warn("Token refresh failed: User '{}' account is disabled", username);
            throw new DisabledException("Account is disabled. Please contact support.");
        }
        log.info("Token refresh successful for user: {}. Issuing new token pair.", username);
        return this.jwtTokenService.generateTokens(user);
    }

    @Override
    public TokenValidationResponse validateToken(final String token) {
        return this.jwtTokenService.validateToken(token);
    }

    private UserAuthResponse fetchUserFromClient(final String username) {
        try {
            return this.userClient.getUserByUsername(username);
        } catch (final FeignException.NotFound _) {
            log.warn("User '{}' not found in user-service", username);
            throw new BadCredentialsException("Invalid username or credentials");
        } catch (final FeignException ex) {
            log.error("Communication failure with user-service for user: {}", username, ex);
            throw new IllegalStateException("Authentication service temporarily unavailable", ex);
        }
    }
}
