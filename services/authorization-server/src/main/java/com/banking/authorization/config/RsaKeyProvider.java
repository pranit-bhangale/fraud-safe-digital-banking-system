package com.banking.authorization.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Getter
@Component
public class RsaKeyProvider {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String RSA_ALGORITHM = "RSA";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider(
            @Value("${banking.security.rsa.key-id:banking-auth-key-1}") final String keyId,
            @Value("${banking.security.rsa.public-key-location}") final String publicKeyLocation,
            @Value("${banking.security.rsa.private-key-location}") final String privateKeyLocation) {

        this.keyId = keyId;

        log.info("Loading RSA KeyPair strictly from locations: KeyId=[{}], Public=[{}], Private=[{}]",
                keyId, publicKeyLocation, privateKeyLocation);

        if (publicKeyLocation == null || publicKeyLocation.isBlank()) {
            throw new IllegalArgumentException("Property 'banking.security.rsa.public-key-location' must be provided");
        }
        if (privateKeyLocation == null || privateKeyLocation.isBlank()) {
            throw new IllegalArgumentException("Property 'banking.security.rsa.private-key-location' must be provided");
        }

        try {
            this.publicKey = loadPublicKey(publicKeyLocation);
            this.privateKey = loadPrivateKey(privateKeyLocation);
            log.info("Successfully loaded RSA KeyPair strictly from locations: Public=[{}], Private=[{}]",
                    publicKeyLocation, privateKeyLocation);
        } catch (final Exception ex) {
            final String message = String.format(
                    "Strict RSA key loading failed. Keys MUST be provided at: Public=[%s], Private=[%s]. Reason: %s",
                    publicKeyLocation, privateKeyLocation, ex.getMessage());
            log.error(message, ex);
            throw new IllegalStateException(message, ex);
        }
    }

    public String getPublicKeyPem() {
        final String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(this.publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n";
    }

    private static RSAPublicKey loadPublicKey(final String pemPath) throws Exception {
        final String rawPem = readContent(pemPath);
        final String cleanKey = rawPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        final byte[] decoded = Base64.getDecoder().decode(cleanKey);
        final var spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(spec);
    }

    private static RSAPrivateKey loadPrivateKey(final String pemPath) throws Exception {
        final String rawPem = readContent(pemPath);
        final String cleanKey = rawPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        final byte[] decoded = Base64.getDecoder().decode(cleanKey);
        final var spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(spec);
    }

    private static String readContent(final String pemPath) throws Exception {
        final String cleanPath = pemPath.startsWith(CLASSPATH_PREFIX)
                ? pemPath.substring(CLASSPATH_PREFIX.length()) : pemPath;
        final Path filePath = Path.of(cleanPath);
        if (Files.exists(filePath)) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }
        final String resourcePath = cleanPath.startsWith("/") ? cleanPath.substring(1) : cleanPath;
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (final InputStream is = (cl != null ? cl : RsaKeyProvider.class.getClassLoader()).getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Key file not found at: " + pemPath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
