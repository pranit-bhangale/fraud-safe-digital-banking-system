package com.banking.security.config;

import com.banking.security.endpoints.PublicEndpointProvider;
import com.banking.security.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
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

    /**
     * Stateless fallback UserDetailsService to prevent UserDetailsServiceAutoConfiguration
     * from auto-generating random in-memory user passwords.
     */
    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Stateless OAuth2 Resource Server: User lookup not supported on client microservices");
        };
    }

    @Bean
    @ConditionalOnMissingBean(PublicEndpointProvider.class)
    public PublicEndpointProvider defaultPublicEndpointProvider() {
        return () -> new String[0];
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain filterChain(
            final HttpSecurity http,
            final List<PublicEndpointProvider> publicEndpointProviders,
            final CorrelationIdFilter correlationIdFilter,
            final JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        log.info("Library Security Configuration: Initializing Resource Server SecurityFilterChain with RBAC");

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
                        .requestMatchers(endpointsArray).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("""
                                    {"status":401,"error":"Unauthorized","message":"Full authentication is required to access this banking resource"}
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("""
                                    {"status":403,"error":"Forbidden","message":"Access denied: Insufficient role permissions"}
                                    """);
                        }))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
