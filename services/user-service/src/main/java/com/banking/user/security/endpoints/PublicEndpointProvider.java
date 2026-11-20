package com.banking.user.security.endpoints;

@FunctionalInterface
public interface PublicEndpointProvider {

    String[] publicEndpoints();
}
