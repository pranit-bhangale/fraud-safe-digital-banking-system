package com.banking.authorization.service;

import com.banking.authorization.dto.AuthResponse;
import com.banking.authorization.dto.LoginRequest;
import com.banking.authorization.dto.TokenValidationResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    TokenValidationResponse validateToken(String token);
}
