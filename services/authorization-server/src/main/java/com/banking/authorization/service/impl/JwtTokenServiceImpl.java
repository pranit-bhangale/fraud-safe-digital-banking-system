package com.banking.authorization.service.impl;

import com.banking.authorization.config.RsaKeyProvider;
import com.banking.authorization.dto.AuthResponse;
import com.banking.authorization.dto.TokenValidationResponse;
import com.banking.authorization.dto.UserAuthResponse;
import com.banking.authorization.service.JwtTokenService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final RsaKeyProvider rsaKeyProvider;
    private final JwtDecoder jwtDecoder;

    @Value("${banking.security.jwt.issuer:authorization-server}")
    private String issuer;

    @Value("${banking.security.jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpirationSeconds;

    @Value("${banking.security.jwt.refresh-token-expiration-seconds:604800}")
    private long refreshTokenExpirationSeconds;

    @Override
    public AuthResponse generateTokens(final UserAuthResponse user) {
        final Instant now = Instant.now();
        final Instant accessExpiresAt = now.plus(this.accessTokenExpirationSeconds, ChronoUnit.SECONDS);
        final Instant refreshExpiresAt = now.plus(this.refreshTokenExpirationSeconds, ChronoUnit.SECONDS);

        final String role = user.role();
        final List<String> roles = List.of(role.startsWith("ROLE_") ? role.substring(5) : role);

        try {
            // 1. Create Access Token (Short-lived)
            final JWTClaimsSet accessClaims = new JWTClaimsSet.Builder()
                    .issuer(this.issuer)
                    .subject(user.id().toString())
                    .claim(CLAIM_USERNAME, user.username())
                    .claim(CLAIM_ROLES, roles)
                    .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(accessExpiresAt))
                    .build();

            final String accessToken = signJwt(accessClaims);

            // 2. Create Refresh Token (Long-lived)
            final JWTClaimsSet refreshClaims = new JWTClaimsSet.Builder()
                    .issuer(this.issuer)
                    .subject(user.id().toString())
                    .claim(CLAIM_USERNAME, user.username())
                    .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(refreshExpiresAt))
                    .build();

            final String refreshToken = signJwt(refreshClaims);

            log.info("Successfully generated access and refresh tokens for user: {}", user.username());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(this.accessTokenExpirationSeconds)
                    .refreshExpiresIn(this.refreshTokenExpirationSeconds)
                    .userId(user.id())
                    .username(user.username())
                    .roles(roles)
                    .build();

        } catch (final Exception ex) {
            log.error("Failed to generate JWT tokens for user: {}", user.username(), ex);
            throw new IllegalStateException("Could not generate JWT tokens", ex);
        }
    }

    @Override
    public String extractUsernameFromRefreshToken(final String refreshToken) {
        final String rawToken = refreshToken.startsWith("Bearer ") ? refreshToken.substring(7) : refreshToken;
        try {
            final Jwt jwt = this.jwtDecoder.decode(rawToken);

            final String tokenType = jwt.getClaimAsString(CLAIM_TOKEN_TYPE);
            if (!TOKEN_TYPE_REFRESH.equalsIgnoreCase(tokenType)) {
                log.warn("Invalid token type for refresh. Found: '{}'", tokenType);
                throw new BadCredentialsException("Provided token is not a refresh token");
            }

            final String username = jwt.getClaimAsString(CLAIM_USERNAME);
            if (username != null && !username.isBlank()) {
                return username;
            }

            return jwt.getSubject();
        } catch (final JwtException ex) {
            log.warn("Refresh token validation failed: {}", ex.getMessage());
            throw new BadCredentialsException("Refresh token is invalid or expired: " + ex.getMessage(), ex);
        }
    }

    @Override
    public TokenValidationResponse validateToken(final String token) {
        final String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        try {
            final Jwt jwt = this.jwtDecoder.decode(rawToken);
            final String subject = jwt.getSubject();
            final String username = jwt.getClaimAsString(CLAIM_USERNAME);
            final List<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);

            UUID userId = null;
            if (subject != null) {
                try {
                    userId = UUID.fromString(subject);
                } catch (final IllegalArgumentException _) {
                }
            }

            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .username(username != null ? username : subject)
                    .roles(roles != null ? roles : List.of())
                    .message("Token is valid")
                    .build();

        } catch (final JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Token is invalid or expired: " + ex.getMessage())
                    .build();
        }
    }

    private String signJwt(final JWTClaimsSet claimsSet) throws Exception {
        final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(this.rsaKeyProvider.getKeyId())
                .build();
        final SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(new RSASSASigner(this.rsaKeyProvider.getPrivateKey()));
        return signedJWT.serialize();
    }
}
