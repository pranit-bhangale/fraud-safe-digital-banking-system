package com.banking.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class JwtAuthConverterConfig {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLES_CLAIM = "roles";
    private static final String PERMISSIONS_CLAIM = "permissions";

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            final List<GrantedAuthority> authorities = new ArrayList<>();
            final List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
            if (roles != null && !roles.isEmpty()) {
                for (final String role : roles) {
                    if (role != null && !role.isBlank()) {
                        final String authority = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                }
            }
            final List<String> permissions = jwt.getClaimAsStringList(PERMISSIONS_CLAIM);
            if (permissions != null && !permissions.isEmpty()) {
                for (final String permission : permissions) {
                    if (permission != null && !permission.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }
                }
            }
            return List.copyOf(authorities);
        });
        return converter;
    }
}
