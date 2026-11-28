package com.banking.gateway.filter;

import com.banking.gateway.dto.GatewayErrorResponse;
import com.banking.gateway.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Spring WebMvc Rate Limiting Filter using Redis Token Bucket.
 * Rejects requests with HTTP 429 Too Many Requests when limits are exceeded.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!rateLimiterService.isAllowed(request)) {
            String clientKey = rateLimiterService.resolveKey(request);
            log.warn("Rate limit exceeded for key: {}, path: {}", clientKey, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            GatewayErrorResponse error = GatewayErrorResponse.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Rate limit exceeded. Please throttle your request volume.")
                    .path(request.getRequestURI())
                    .timestamp(Instant.now())
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
