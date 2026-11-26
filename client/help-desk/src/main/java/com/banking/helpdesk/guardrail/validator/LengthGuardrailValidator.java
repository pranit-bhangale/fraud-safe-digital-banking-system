package com.banking.helpdesk.guardrail.validator;

import com.banking.helpdesk.guardrail.model.GuardrailResult;
import com.banking.helpdesk.guardrail.model.GuardrailViolation;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import com.banking.helpdesk.guardrail.properties.GuardrailProperties;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cheapest check, runs first: rejects oversized input before any regex runs
 * (prompt stuffing, token-cost abuse, regex DoS).
 */
@Component
public class LengthGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;

    public LengthGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT;
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (text == null || text.length() <= properties.maxInputLength()) return GuardrailResult.success(text);
        String msg = properties.inputViolationMessage();
        return GuardrailResult.failure(
                List.of(new GuardrailViolation(phase, "INPUT_TOO_LONG", "max=" + properties.maxInputLength(), msg)),
                msg);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}