package com.banking.payment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Payment Gateway Service API",
                version = "v1",
                description = "Production-grade merchant payment order creation, checkout processing, and HMAC-signed webhook notification management.",
                contact = @Contact(name = "Digital Banking Architecture Team", email = "architecture@bank.com")
        ),
        security = @SecurityRequirement(name = "BearerAuth")
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter RSA-256 JWT Bearer access token issued by User Service."
)
public class OpenApiConfig {

    @Bean
    public OperationCustomizer customizeApiVersionHeader() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(
                    new Parameter()
                            .name("X-API-Version")
                            .in("header")
                            .required(false)
                            .description("API Version header (e.g. v1). Defaults to 'v1' if omitted.")
                            .example("v1")
            );
            return operation;
        };
    }
}
