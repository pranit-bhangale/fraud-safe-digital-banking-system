package com.pranit.praya.guardrail.validator;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.ValidationPhase;
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
