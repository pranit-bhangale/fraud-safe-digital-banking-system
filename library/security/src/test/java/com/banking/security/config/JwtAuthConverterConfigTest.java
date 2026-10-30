package com.banking.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthConverterConfigTest {

    private final JwtAuthConverterConfig config = new JwtAuthConverterConfig();

    @Test
    @DisplayName("Should extract roles (with ROLE_ prefix) and permissions (direct authority)")
    void shouldExtractRolesAndPermissions() {
        final JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        final Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", "usr-123")
                .claim("roles", List.of("CUSTOMER", "ROLE_ADMIN"))
                .claim("permissions", List.of("account:read", "transfer:execute"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        final AbstractAuthenticationToken auth = converter.convert(jwt);
        assertNotNull(auth);

        final Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertTrue(authorities.containsAll(Set.of(
                "ROLE_CUSTOMER",
                "ROLE_ADMIN",
                "account:read",
                "transfer:execute"
        )));
        assertTrue(authorities.contains("ROLE_CUSTOMER"));
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("account:read"));
        assertTrue(authorities.contains("transfer:execute"));
    }
}
