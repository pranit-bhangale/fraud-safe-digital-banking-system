package com.banking.helpdesk.guardrail.exception;

import com.banking.helpdesk.guardrail.model.GuardrailViolation;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when prompt or response validation violates guardrail criteria in strict mode.
 */
@Getter
public class GuardrailViolationException extends RuntimeException {

    private final ValidationPhase phase;
    private final List<GuardrailViolation> violations;

    public GuardrailViolationException(ValidationPhase phase, String message) {
        super(message);
        this.phase = phase;
        this.violations = List.of(new GuardrailViolation(phase, "STRICT_GUARDRAIL", "", message));
    }

    public GuardrailViolationException(ValidationPhase phase, List<GuardrailViolation> violations, String message) {
        super(message);
        this.phase = phase;
        this.violations = violations != null ? List.copyOf(violations) : List.of();
    }
}
