package com.banking.security.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.*;

class LoadKeyTest {

    @Test
    @DisplayName("Should successfully load keys/public_key.pem using LoadKey")
    void shouldLoadPublicKey() throws Exception {
        final RSAPublicKey publicKey = LoadKey.loadPublicKey("keys/public_key.pem");
        assertNotNull(publicKey);
        assertEquals("RSA", publicKey.getAlgorithm());
    }

    @Test
    @DisplayName("Should load public key with classpath: prefix")
    void shouldLoadPublicKeyWithClasspathPrefix() throws Exception {
        final RSAPublicKey publicKey = LoadKey.loadPublicKey("classpath:keys/public_key.pem");
        assertNotNull(publicKey);
        assertEquals("RSA", publicKey.getAlgorithm());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when key file is missing")
    void shouldThrowWhenFileNotFound() {
        assertThrows(IllegalArgumentException.class, () -> LoadKey.loadPublicKey("keys/missing.pem"));
    }

    @Test
    @DisplayName("Should throw NullPointerException or IllegalArgumentException on null/blank path")
    void shouldThrowOnNullOrBlank() {
        assertThrows(NullPointerException.class, () -> LoadKey.loadPublicKey(null));
        assertThrows(IllegalArgumentException.class, () -> LoadKey.loadPublicKey("   "));
    }
}
