package com.banking.security.helper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityContextTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return null when unauthenticated")
    void shouldReturnNullWhenUnauthenticated() {
        assertNull(SecurityContext.getUserIdFromSecurityContext());
        assertNull(SecurityContext.getUsernameFromSecurityContext());
        assertTrue(SecurityContext.getRolesFromSecurityContext().isEmpty());
        assertFalse(SecurityContext.hasRole("ADMIN"));
    }

    @Test
    @DisplayName("Should resolve user UUID, username, and roles from active JWT")
    void shouldResolveUserDetails() {
        final UUID expectedUuid = UUID.randomUUID();
        final Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", expectedUuid.toString())
                .claim("username", "banking_user")
                .build();

        final JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("account:read")),
                expectedUuid.toString()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(expectedUuid, SecurityContext.getUserIdFromSecurityContext());
        assertEquals("banking_user", SecurityContext.getUsernameFromSecurityContext());
        assertTrue(SecurityContext.hasRole("USER"));
        assertTrue(SecurityContext.hasRole("ROLE_USER"));
        assertFalse(SecurityContext.hasRole("ADMIN"));

        final List<String> roles = SecurityContext.getRolesFromSecurityContext();
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("account:read"));
    }
}
