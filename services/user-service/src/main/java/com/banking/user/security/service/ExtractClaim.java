package com.banking.user.security.service;

import io.jsonwebtoken.Claims;

public interface ExtractClaim {

    String getUsernameFromAccessToken(Claims claims);

    Claims validateAndParseToken(String token);

    boolean isAccessToken(Claims claims);
}
