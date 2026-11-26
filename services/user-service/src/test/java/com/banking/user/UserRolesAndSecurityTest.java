package com.banking.user;

import com.banking.user.dto.RegistrationRequest;
import com.banking.user.entity.Role;
import com.banking.user.entity.User;
import com.banking.user.model.RoleType;
import com.banking.user.model.UserDetail;
import com.banking.user.publisher.EventPublisher;
import com.banking.user.repository.RoleRepository;
import com.banking.user.repository.UserRepository;
import com.banking.user.security.service.ExtractClaim;
import com.banking.user.security.service.GenerateToken;
import com.banking.user.security.store.TokenBlacklist;
import com.banking.user.service.impl.AuthServiceImpl;
import com.banking.user.service.impl.UserDetailServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRolesAndSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private GenerateToken generateToken;

    @Mock
    private com.banking.user.store.TokenBlacklist tokenBlacklist;

    @Mock
    private ExtractClaim extractClaim;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Role Assignment: Successfully registers new user with default CUSTOMER role")
    void shouldRegisterWithDefaultCustomerRole() {
        RegistrationRequest request = new RegistrationRequest(
                "Jane", "Doe", "janedoe", "jane@example.com",
                "StrongP@ss123", "StrongP@ss123", RoleType.CUSTOMER
        );

        Role customerRole = Role.builder().id((short) 3).name(RoleType.CUSTOMER).build();

        when(userRepository.existsByUsername("janedoe")).thenReturn(false);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");
        when(roleRepository.findByName(RoleType.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        authService.createNewAccount(request);

        verify(roleRepository).findByName(RoleType.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("UserDetail Authorities: Correctly maps roles to ROLE_ prefixed Spring GrantedAuthorities")
    void shouldMapRolesToGrantedAuthorities() {
        UserDetail userDetail = UserDetail.builder()
                .id(UUID.randomUUID())
                .username("admin_user")
                .roles(Set.of("ADMIN", "MANAGER"))
                .build();

        Set<String> authorities = userDetail.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(2, authorities.size());
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_MANAGER"));
    }
}
