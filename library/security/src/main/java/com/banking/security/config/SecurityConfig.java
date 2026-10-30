package com.banking.security.config;

import com.banking.security.endpoints.PublicEndpointProvider;
import com.banking.security.filter.CorrelationIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] DEFAULT_PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain filterChain(
            final HttpSecurity http,
            final List<PublicEndpointProvider> publicEndpointProviders,
            final CorrelationIdFilter correlationIdFilter) {
        log.info("Library Security Configuration: Initializing SecurityFilterChain");
        final Set<String> allEndpoints = new LinkedHashSet<>(Arrays.asList(DEFAULT_PUBLIC_ENDPOINTS));
        if (publicEndpointProviders != null) {
            for (final PublicEndpointProvider provider : publicEndpointProviders) {
                if (provider != null) {
                    final String[] endpoints = provider.publicEndpoints();
                    if (endpoints != null && endpoints.length > 0) {
                        allEndpoints.addAll(Arrays.asList(endpoints));
                    }
                }
            }
        }
        final String[] endpointsArray = allEndpoints.toArray(String[]::new);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request -> request
                        .requestMatchers(endpointsArray)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
