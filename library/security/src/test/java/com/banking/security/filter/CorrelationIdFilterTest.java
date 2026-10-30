package com.banking.security.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should propagate incoming X-Correlation-ID header to response and MDC")
    void shouldPropagateExistingCorrelationId() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "corr-test-123");

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals("corr-test-123", response.getHeader("X-Correlation-ID"));
        assertNull(MDC.get("X-Correlation-ID"), "MDC must be cleared after filter execution");
    }

    @Test
    @DisplayName("Should generate a new UUID correlation ID when header is absent")
    void shouldGenerateNewCorrelationIdWhenAbsent() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        final String generatedId = response.getHeader("X-Correlation-ID");
        assertNotNull(generatedId);
        assertFalse(generatedId.isBlank());
        assertNull(MDC.get("X-Correlation-ID"), "MDC must be cleared after filter execution");
    }
}
