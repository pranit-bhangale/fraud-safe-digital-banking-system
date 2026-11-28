package com.banking.payment.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for HMAC-SHA256 signature generation for secure webhooks.
 */
@Slf4j
@Component
public class HmacSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String generateSignature(String payload, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC-SHA256 signature: {}", e.getMessage(), e);
            throw new RuntimeException("Cryptographic signature failure", e);
        }
    }
}
