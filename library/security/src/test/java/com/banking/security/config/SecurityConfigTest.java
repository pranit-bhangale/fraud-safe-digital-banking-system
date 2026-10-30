package com.banking.security.config;

import com.banking.security.endpoints.PublicEndpointProvider;
import com.banking.security.filter.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class,
                    SecurityConfig.class,
                    JwtConfig.class,
                    JwtAuthConverterConfig.class,
                    CorrelationFilterConfig.class
            ));

    @Test
    @DisplayName("Should auto-configure security filter chain when client provides public-key-location in YAML")
    void shouldAutoConfigureWhenClientProvidesConfig() {
        contextRunner
                .withPropertyValues("spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:keys/public_key.pem")
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                    assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
                    assertThat(context).hasSingleBean(CorrelationIdFilter.class);
                });
    }

    @Test
    @DisplayName("Strict validation: should fail startup when client omits public-key-location")
    void shouldFailStartupWhenPropertyIsMissing() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasFailed();
                });
    }

    @Test
    @DisplayName("Should wire custom PublicEndpointProvider and combine with default platform endpoints")
    void shouldWireCustomEndpointProvider() {
        contextRunner
                .withPropertyValues("spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:keys/public_key.pem")
                .withUserConfiguration(CustomEndpointConfig.class)
                .run(context -> {
                    assertThat(context).hasBean("customEndpoints");
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                });
    }

    @Configuration
    static class CustomEndpointConfig {
        @Bean
        public PublicEndpointProvider customEndpoints() {
            return () -> new String[]{"/api/v1/auth/login", "/api/v1/auth/register"};
        }
    }
}
