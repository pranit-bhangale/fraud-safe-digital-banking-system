package com.banking.helpdesk.guardrail.validator;

import com.banking.helpdesk.guardrail.model.GuardrailResult;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import org.springframework.core.Ordered;

/**
 * Strategy interface for guardrail validators.
 */
public interface GuardrailValidator extends Ordered {

    boolean supports(ValidationPhase phase);

    GuardrailResult validate(String text, ValidationPhase phase);

    @Override
    default int getOrder() {
        return 0;
    }
}
