package com.banking.user.dto;

public record UserRequest(
        String username,
        String password
) {
}
