package com.banking.user.service.impl;

import com.banking.user.dto.AuthenticatedUser;
import com.banking.user.dto.LoginRequest;
import com.banking.user.dto.LoginResponse;
import com.banking.user.dto.RegistrationRequest;
import com.banking.user.dto.TokenResponse;
import com.banking.user.entity.User;
import com.banking.user.event.NotificationEvent;
import com.banking.user.exception.InvalidRequestException;
import com.banking.user.exception.UnauthorizedException;
import com.banking.user.model.UserDetail;
import com.banking.user.publisher.EventPublisher;
import com.banking.user.repository.UserRepository;
import com.banking.user.security.context.SecurityContext;
import com.banking.user.security.service.ExtractClaim;
import com.banking.user.security.service.GenerateToken;
import com.banking.user.service.AuthService;
import com.banking.user.store.TokenBlacklist;
import com.banking.utility.service.Generate;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Duration CLOCK_SKEW_BUFFER = Duration.ofMinutes(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final AuthenticationManager authenticationManager;
    private final GenerateToken generateToken;
    private final TokenBlacklist tokenBlacklist;
    private final ExtractClaim extractClaim;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createNewAccount(RegistrationRequest request) {
        if (this.userRepository.existsByUsername(request.username())) {
            throw new InvalidRequestException("Username is already in use");
        }
        if (this.userRepository.existsByEmail(request.email())) {
            throw new InvalidRequestException("Email is already in use");
        }
        User user = User.builder()
                .username(request.username())
                .password(this.passwordEncoder.encode(request.password()))
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();
        user = this.userRepository.save(user);
        final Map<String, Object> data = Map.of(
                "organizationName", "Digital Banking",
                "organizationUrl", "https://banking.com",
                "fullName", user.getFirstName() + " " + user.getLastName(),
                "username", user.getUsername(),
                "createdAt", Instant.now().toString()
        );
        final NotificationEvent event = NotificationEvent.builder()
                .eventId(Generate.eventId())
                .type("WELCOME")
                .source("user-service")
                .occurredAt(Instant.now())
                .email(user.getEmail())
                .data(data)
                .build();
        this.eventPublisher.processEmail(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public LoginResponse authenticateUser(LoginRequest request) {
        final UserDetail authenticatedUser = authenticate(request);
        final TokenResponse token = this.generateToken.generateAccessToken(authenticatedUser);
        return LoginResponse.builder()
                .username(authenticatedUser.username())
                .accessToken(token.accessToken())
                .tokenType(token.tokenType())
                .expiresIn(token.expiresIn())
                .build();
    }

    @Override
    public AuthenticatedUser getCurrentUser() {
        final UserDetail principal = SecurityContext.getUserDetail();
        return AuthenticatedUser.builder()
                .id(principal.id())
                .username(principal.username())
                .email(principal.email())
                .roles(principal.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();
    }


    @Override
    public void logout(final String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRequestException("Access token is required");
        }
        final Claims claims;
        try {
            claims = this.extractClaim.validateAndParseToken(token);
        } catch (final JwtException e) {
            throw new InvalidRequestException("Invalid or expired access token");
        }
        if (!this.extractClaim.isAccessToken(claims)) {
            throw new InvalidRequestException("Only access tokens can be revoked");
        }
        final Date expiration = claims.getExpiration();
        if (expiration == null) return;
        final Duration ttl = Duration.between(Instant.now(), expiration.toInstant()).plus(CLOCK_SKEW_BUFFER);
        if (ttl.isNegative() || ttl.isZero()) return;
        this.tokenBlacklist.blacklist(token, ttl);
    }

    private UserDetail authenticate(LoginRequest request) {
        try {
            final Authentication authentication = this.authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(),
                            request.password())
            );
            if (!(authentication.getPrincipal() instanceof UserDetail userDetail)) {
                throw new UnauthorizedException("Invalid authentication principal");
            }
            return userDetail;
        } catch (BadCredentialsException exception) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
