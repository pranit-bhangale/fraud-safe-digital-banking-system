package com.banking.account.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("internalSecurityEvaluator")
@RequiredArgsConstructor
public class InternalSecurityEvaluator {

    private final HttpServletRequest request;

    public boolean isInternalRequest() {
        String internalHeader = request.getHeader("X-Internal-Service");
        return internalHeader != null && !internalHeader.isBlank();
    }
}
