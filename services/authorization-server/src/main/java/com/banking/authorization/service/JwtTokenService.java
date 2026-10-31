package com.banking.authorization.service;

import com.banking.authorization.dto.AuthResponse;
import com.banking.authorization.dto.TokenValidationResponse;
import com.banking.authorization.dto.UserAuthResponse;

public interface JwtTokenService {

    AuthResponse generateTokens(UserAuthResponse user);

    TokenValidationResponse validateToken(String token);

    String extractUsernameFromRefreshToken(String refreshToken);
}
