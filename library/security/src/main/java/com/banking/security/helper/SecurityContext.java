package com.banking.security.helper;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility class providing helper methods to retrieve user context from the active Spring Security context.
 */
public final class SecurityContext {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String USERNAME_CLAIM = "username";

    private SecurityContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Resolves the authenticated user's {@link UUID} from the JWT subject ("sub").
     *
     * @return {@link UUID} if authenticated and subject is a valid UUID, otherwise {@code null}
     */
    public static UUID getUserIdFromSecurityContext() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            final String subject = jwt.getSubject();
            if (subject != null && !subject.isBlank()) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException _) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Resolves the username from the JWT claims ("username") or authentication token.
     *
     * @return username string if authenticated, otherwise {@code null}
     */
    public static String getUsernameFromSecurityContext() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            if (jwt.hasClaim(USERNAME_CLAIM)) return jwt.getClaimAsString(USERNAME_CLAIM);
            return jwt.getSubject();
        }
        return authentication.getName();
    }

    /**
     * Checks if the currently authenticated principal possesses the given role.
     *
     * @param role role to check (e.g. "ADMIN" or "ROLE_ADMIN")
     * @return {@code true} if present, {@code false} otherwise
     */
    public static boolean hasRole(final String role) {
        if (role == null || role.isBlank()) return false;
        final List<String> roles = getRolesFromSecurityContext();
        final String prefixedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return roles.contains(role) || roles.contains(prefixedRole);
    }

    /**
     * Retrieves all granted authorities/roles from the active security context.
     *
     * @return unmodifiable list of role/authority strings
     */
    public static List<String> getRolesFromSecurityContext() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        } else {
            authentication.getAuthorities();
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .toList();
    }
}
