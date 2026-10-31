package com.banking.user.service;

import com.banking.user.dto.UserRequest;
import jakarta.validation.Valid;

public interface UserService {
    
    void createUserAccount(@Valid UserRequest request);
}
