package com.banking.user.security.service;


import com.banking.user.dto.TokenResponse;
import com.banking.user.model.UserDetail;

public interface GenerateToken {

    TokenResponse generateAccessToken(UserDetail userDetail);
}