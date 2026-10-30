package com.banking.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    @DisplayName("Should successfully load keys/public_key.pem and create JwtDecoder")
    void shouldLoadBundledPublicKey() throws Exception {
        ReflectionTestUtils.setField(jwtConfig, "publicKeyLocation", "classpath:keys/public_key.pem");
        final JwtDecoder decoder = jwtConfig.jwtDecoder();
        assertNotNull(decoder);
    }
}
