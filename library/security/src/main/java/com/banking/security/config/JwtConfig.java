package com.banking.security.config;

import com.banking.security.util.LoadKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

/**
 * Production-ready JWT configuration with automatic fail-safe resolution:
 * Resolves public key from application.yaml property, or falls back to standard classpath/filesystem location.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.public-key-location:key/public_key.pem}")
            String publicKeyLocation) throws Exception {
        log.info("Initializing JWT Decoder with public key location: {}", publicKeyLocation);
        RSAPublicKey publicKey;
        try {
            publicKey = LoadKey.loadPublicKey(publicKeyLocation);
        } catch (Exception ex) {
            log.warn("Could not load public key from primary path {}. Attempting classpath fallback: classpath:key/public_key.pem",
                    publicKeyLocation);
            publicKey = LoadKey.loadPublicKey("classpath:key/public_key.pem");
        }
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
