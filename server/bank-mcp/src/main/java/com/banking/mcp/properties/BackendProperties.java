package com.banking.mcp.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "backend")
@Validated
public record BackendProperties(@Valid Map<String, ClientConfig> clients) {

    public record ClientConfig(
            @NotBlank String baseUrl,
            @DefaultValue("2s") Duration connectTimeout,
            @DefaultValue("5s") Duration readTimeout,
            @DefaultValue("1s") Duration connectionRequestTimeout,
            @DefaultValue("5m") Duration connectionTtl,
            @DefaultValue("30s") Duration idleEviction,
            @DefaultValue("50") @Min(1) int maxConnections,
            @DefaultValue("25") @Min(1) int maxPerRoute
    ) {
    }
}