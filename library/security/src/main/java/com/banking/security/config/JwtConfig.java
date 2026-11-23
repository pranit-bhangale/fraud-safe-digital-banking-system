package com.banking.security.config;

import com.banking.security.util.LoadKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

/**
 * Strict JWT configuration: requires the client microservice to provide
 * {@code spring.security.oauth2.resourceserver.jwt.public-key-location} in their application.yaml.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(
            prefix = "spring.security.oauth2.resourceserver.jwt",
            name = "public-key-location"
    )
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location}")
            String publicKeyLocation) throws Exception {
        final RSAPublicKey publicKey = LoadKey.loadPublicKey(publicKeyLocation);
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
