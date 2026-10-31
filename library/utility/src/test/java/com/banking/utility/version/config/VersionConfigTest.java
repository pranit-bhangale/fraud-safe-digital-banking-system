package com.banking.utility.version.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VersionConfigTest {

    private final VersionConfig config = new VersionConfig();

    @Test
    @DisplayName("Should successfully configure API versioning on ApiVersionConfigurer")
    void shouldConfigureApiVersioning() {
        final ApiVersionConfigurer configurer = new ApiVersionConfigurer();
        assertDoesNotThrow(() -> config.configureApiVersioning(configurer));
        assertNotNull(configurer);
    }
}
