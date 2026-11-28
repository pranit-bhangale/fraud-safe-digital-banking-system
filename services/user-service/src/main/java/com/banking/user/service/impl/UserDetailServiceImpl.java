package com.banking.user.service.impl;

import com.banking.user.entity.User;
import com.banking.user.model.UserDetail;
import com.banking.user.repository.UserRepository;
import com.banking.user.service.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public @NullMarked UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        final Set<String> roles = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())
                : Set.of("CUSTOMER");

        return UserDetail.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
