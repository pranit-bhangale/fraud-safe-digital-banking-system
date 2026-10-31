package com.banking.user.service.impl;

import com.banking.user.dto.UserAuthResponse;
import com.banking.user.dto.UserRequest;
import com.banking.user.entity.Role;
import com.banking.user.entity.User;
import com.banking.user.exception.UserAlreadyExistsException;
import com.banking.user.repository.UserRepository;
import com.banking.user.service.UserService;
import com.banking.utility.annotation.LogExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @LogExecution
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createUserAccount(final UserRequest request) {
        this.userRepository.findByUsername(request.username())
                .ifPresent(_ -> {
                    throw new UserAlreadyExistsException("User already exists");
                });
        final User user = User.builder()
                .username(request.username())
                .password(this.passwordEncoder.encode(request.password()))
                .enabled(true)
                .role(Role.USER)
                .build();
        this.userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public UserAuthResponse getUserDataByUsername(final String username) {
        final User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserAuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .enabled(user.isEnabled())
                .role(String.valueOf(user.getRole()))
                .build();
    }
}
