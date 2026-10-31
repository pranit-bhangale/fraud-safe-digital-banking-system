package com.banking.authorization.service.impl;

import com.banking.authorization.client.UserClient;
import com.banking.authorization.dto.UserAuthResponse;
import com.banking.authorization.service.UserDetailService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailService {
    private final UserClient userClient;

    @Override
    public @NonNull UserDetails loadUserByUsername(final @NonNull String username) throws UsernameNotFoundException {
        log.info("Fetching user details for username: {} from user-service via Feign", username);
        try {
            final UserAuthResponse user = this.userClient.getUserByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            final String role = user.role();
            final String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            return User.builder()
                    .username(user.username())
                    .password(user.password())
                    .disabled(!user.enabled())
                    .authorities(List.of(new SimpleGrantedAuthority(roleWithPrefix)))
                    .build();
        } catch (final FeignException.NotFound _) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        } catch (final FeignException ex) {
            log.error("Error communicating with user-service for user: {}", username, ex);
            throw new AuthenticationServiceException("Failed to verify user credentials with user-service", ex);
        }
    }
}