package com.banking.security.endpoints;

@FunctionalInterface
public interface PublicEndpointProvider {

    String[] publicEndpoints();
}
