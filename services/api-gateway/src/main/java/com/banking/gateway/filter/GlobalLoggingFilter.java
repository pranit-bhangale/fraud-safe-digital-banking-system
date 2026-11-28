package com.banking.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Spring WebMvc Global Filter for:
 * 1. Generating or propagating X-Correlation-Id tracing headers.
 * 2. Enforcing and propagating X-API-Version headers (defaults to 'v1' if omitted).
 * 3. Stripping untrusted internal security headers from public edge ingress.
 * 4. Logging request metadata and execution latency in milliseconds.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String API_VERSION_HEADER = "X-API-Version";
    public static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service";
    public static final String DEFAULT_API_VERSION = "v1";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String apiVersion = request.getHeader(API_VERSION_HEADER);
        if (apiVersion == null || apiVersion.isBlank()) {
            apiVersion = DEFAULT_API_VERSION;
        }

        // Set tracing & version headers on response
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(API_VERSION_HEADER, apiVersion);

        // Sanitize headers: wrap request to inject defaults and strip spoofed internal headers
        HeaderSanitizingRequestWrapper requestWrapper = new HeaderSanitizingRequestWrapper(request);
        requestWrapper.setHeader(CORRELATION_ID_HEADER, correlationId);
        requestWrapper.setHeader(API_VERSION_HEADER, apiVersion);
        requestWrapper.removeHeader(INTERNAL_SERVICE_HEADER);

        try {
            filterChain.doFilter(requestWrapper, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("GATEWAY_ACCESS: method={}, path={}, version={}, status={}, latency={}ms, correlationId={}",
                    request.getMethod(), request.getRequestURI(), apiVersion, response.getStatus(), duration, correlationId);
        }
    }

    private static class HeaderSanitizingRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new HashMap<>();
        private final Set<String> removedHeaders = new HashSet<>();

        public HeaderSanitizingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void setHeader(String name, String value) {
            customHeaders.put(name.toLowerCase(Locale.ROOT), value);
            removedHeaders.remove(name.toLowerCase(Locale.ROOT));
        }

        public void removeHeader(String name) {
            removedHeaders.add(name.toLowerCase(Locale.ROOT));
            customHeaders.remove(name.toLowerCase(Locale.ROOT));
        }

        @Override
        public String getHeader(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (removedHeaders.contains(lower)) {
                return null;
            }
            if (customHeaders.containsKey(lower)) {
                return customHeaders.get(lower);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (removedHeaders.contains(lower)) {
                return Collections.emptyEnumeration();
            }
            if (customHeaders.containsKey(lower)) {
                return Collections.enumeration(List.of(customHeaders.get(lower)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>();
            Enumeration<String> parentNames = super.getHeaderNames();
            while (parentNames.hasMoreElements()) {
                String name = parentNames.nextElement();
                if (!removedHeaders.contains(name.toLowerCase(Locale.ROOT))) {
                    names.add(name);
                }
            }
            names.addAll(customHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}
