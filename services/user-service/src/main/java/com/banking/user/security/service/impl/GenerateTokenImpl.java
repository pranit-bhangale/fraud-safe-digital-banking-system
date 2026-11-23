package com.banking.user.security.service.impl;

import com.banking.user.dto.TokenResponse;
import com.banking.user.model.UserDetail;
import com.banking.user.properties.KeysPathProperties;
import com.banking.user.properties.TokenProperties;
import com.banking.user.security.load.LoadKey;
import com.banking.user.security.service.GenerateToken;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenerateTokenImpl implements GenerateToken {

    private final TokenProperties tokenProperties;
    private final PrivateKey privateKey;

    public GenerateTokenImpl(TokenProperties tokenProperties, KeysPathProperties keysPathProperties) {
        this.tokenProperties = tokenProperties;
        this.privateKey = LoadKey.loadPrivateKey(keysPathProperties.privateKeyPath());
    }

    @Override
    public TokenResponse generateAccessToken(final UserDetail userDetail) {
        final UUID userId = userDetail.id();
        final String username = userDetail.username();
        final Instant now = Instant.now();
        final Instant expiry = now.plusSeconds(tokenProperties.accessToken().expiration());
        final Set<String> roles = userDetail.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(Collectors.toUnmodifiableSet());
        final String accessToken = Jwts.builder()
                .id(userId.toString())
                .subject(username)
                .issuer(tokenProperties.token().issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("roles", roles)
                .claim("type", "access")
                .signWith(this.privateKey)
                .compact();
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer ")
                .expiresIn(this.tokenProperties.accessToken().expiration())
                .build();
    }
}
