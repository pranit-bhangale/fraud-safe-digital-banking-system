package com.banking.user.security.endpoints.impl;

import com.banking.user.security.endpoints.PublicEndpointProvider;
import org.springframework.stereotype.Service;

@Service
public class PublicEndpointProviderImpl implements PublicEndpointProvider {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/api/auth/register",
            "/api/auth/login",
            "/internal/users/**"
    };

    @Override
    public String[] publicEndpoints() {
        return PUBLIC_ENDPOINTS;
    }
}
