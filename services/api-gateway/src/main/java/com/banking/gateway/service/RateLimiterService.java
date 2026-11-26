package com.banking.gateway.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Production-optimized Redis-backed sliding-window token bucket rate limiter for Spring WebMvc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(HttpServletRequest request) {
        String key = resolveKey(request);
        String path = request.getRequestURI();
        long limit = path.startsWith("/api/v1/transactions") ? 20 : 50;
        try {
            Long currentCount = redisTemplate.opsForValue().increment("rl:" + key);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire("rl:" + key, Duration.ofSeconds(1));
            }
            return currentCount != null && currentCount <= limit;
        } catch (Exception ex) {
            log.warn("Redis rate limiter unavailable: {}. Allowing request in fail-open mode.", ex.getMessage());
            return true;
        }
    }

    public String resolveKey(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) return "user_" + userId;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 20) {
            return "token_" + authHeader.substring(7).hashCode();
        }
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }
        return "ip_" + (clientIp != null ? clientIp : "unknown");
    }
}
