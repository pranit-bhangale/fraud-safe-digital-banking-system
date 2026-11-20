package com.banking.user.security.service.impl;

import com.banking.user.properties.KeysPathProperties;
import com.banking.user.security.load.LoadKey;
import com.banking.user.security.service.ExtractClaim;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

@Slf4j
@Service
public class ExtractClaimImpl implements ExtractClaim {

    private final PublicKey publicKey;

    public ExtractClaimImpl(KeysPathProperties keysPathProperties) {
        this.publicKey = LoadKey.loadPublicKey(keysPathProperties.publicKeyPath());
    }

    @Override
    public String getUsernameFromAccessToken(final Claims claims) {
        return claims.getSubject();
    }
    
    @Override
    public Claims validateAndParseToken(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            throw new JwtException("Invalid or expired JWT", ex);
        }
    }

    @Override
    public boolean isAccessToken(final Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }
}
