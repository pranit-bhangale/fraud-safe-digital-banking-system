package com.banking.account.security;

import com.banking.security.endpoints.PublicEndpointProvider;
import org.springframework.stereotype.Component;

@Component
public class AccountPublicEndpointProvider implements PublicEndpointProvider {

    @Override
    public String[] publicEndpoints() {
        return new String[] {
                "/api/accounts/*/balance/**"
        };
    }
}
