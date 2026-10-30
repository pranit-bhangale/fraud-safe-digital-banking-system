package com.banking.security.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * Production-ready utility for loading RSA public keys from PEM files
 * (filesystem or classpath) using modern Java 25 capabilities.
 */
public final class LoadKey {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String BEGIN_MARKER = "-----BEGIN PUBLIC KEY-----";
    private static final String END_MARKER = "-----END PUBLIC KEY-----";
    private static final String RSA_ALGORITHM = "RSA";

    private LoadKey() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Loads an {@link RSAPublicKey} from a PEM file path (filesystem or classpath).
     *
     * @param pemPath path to the PEM file, optionally prefixed with {@code classpath:}
     * @return initialized {@link RSAPublicKey}
     * @throws IOException              if the key file cannot be read
     * @throws NoSuchAlgorithmException if RSA algorithm is unavailable
     * @throws InvalidKeySpecException  if the key specification is invalid
     */
    public static RSAPublicKey loadPublicKey(final String pemPath)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Objects.requireNonNull(pemPath, "PEM key path must not be null");
        if (pemPath.isBlank()) {
            throw new IllegalArgumentException("PEM key path must not be blank");
        }
        final String rawPem = readKeyContent(pemPath);
        final String cleanKey = rawPem
                .replace(BEGIN_MARKER, "")
                .replace(END_MARKER, "")
                .strip();
        final byte[] decoded = Base64.getMimeDecoder().decode(cleanKey);
        final var spec = new X509EncodedKeySpec(decoded);
        final var keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    private static String readKeyContent(final String pemPath) throws IOException {
        final String cleanPath = pemPath.startsWith(CLASSPATH_PREFIX)
                ? pemPath.substring(CLASSPATH_PREFIX.length()) : pemPath;
        final Path filePath = Path.of(cleanPath);
        if (Files.exists(filePath)) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        }
        final String resourcePath = cleanPath.startsWith("/")
                ? cleanPath.substring(1) : cleanPath;
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader classLoader = contextClassLoader != null
                ? contextClassLoader : LoadKey.class.getClassLoader();
        try (final InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Public key resource not found at: " + pemPath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
