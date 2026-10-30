package com.banking.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@NullMarked
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(CORRELATION_ID);
        final String correlationId = (header == null || header.isBlank())
                ? UUID.randomUUID().toString() : header.strip();
        MDC.put(CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }
}
