package com.banking.user.config;

import com.banking.security.endpoints.PublicEndpointProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PublicEndpointProvider publicEndpointProvider() {
        return () -> new String[]{
                "/api/users/create",
                "/api/users/internal/**"
        };
    }
}
