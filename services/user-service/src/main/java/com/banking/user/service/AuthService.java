package com.banking.user.service;

import com.banking.user.dto.AuthenticatedUser;
import com.banking.user.dto.LoginRequest;
import com.banking.user.dto.LoginResponse;
import com.banking.user.dto.RegistrationRequest;
import jakarta.validation.Valid;

public interface AuthService {

    void createNewAccount(@Valid RegistrationRequest request);

    LoginResponse authenticateUser(@Valid LoginRequest request);

    AuthenticatedUser getCurrentUser();

    void logout(String authorization);
}
