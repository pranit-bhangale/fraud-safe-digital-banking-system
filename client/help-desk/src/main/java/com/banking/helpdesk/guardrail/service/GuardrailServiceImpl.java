package com.banking.helpdesk.guardrail.service;

import com.banking.helpdesk.guardrail.exception.GuardrailViolationException;
import com.banking.helpdesk.guardrail.model.GuardrailResult;
import com.banking.helpdesk.guardrail.model.GuardrailViolation;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import com.banking.helpdesk.guardrail.normalizer.TextNormalizer;
import com.banking.helpdesk.guardrail.properties.GuardrailProperties;
import com.banking.helpdesk.guardrail.validator.GuardrailValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite orchestrator executing all registered {@link GuardrailValidator} strategies.
 * Fail-closed: if a validator itself fails (bug, regex blow-up), the text is treated as violating.
 */
@Slf4j
@Service
public class GuardrailServiceImpl implements GuardrailService {

    private final GuardrailProperties properties;
    private final List<GuardrailValidator> validators;

    public GuardrailServiceImpl(GuardrailProperties properties, List<GuardrailValidator> validators) {
        this.properties = properties;
        this.validators = new ArrayList<>(validators);
        AnnotationAwareOrderComparator.sort(this.validators);
    }

    @Override
    public GuardrailResult evaluate(String text, ValidationPhase phase) {
        if (!this.properties.enabled()) return GuardrailResult.success(text);
        final String normalized = TextNormalizer.normalize(text);
        final List<GuardrailViolation> aggregatedViolations = new ArrayList<>();
        for (GuardrailValidator validator : this.validators) {
            if (!validator.supports(phase)) continue;
            GuardrailResult result = runValidator(validator, normalized, phase);
            if (result.valid()) continue;
            aggregatedViolations.addAll(result.violations());
            log.warn("[GUARDRAIL] [CHECK-FAILED] phase: {}, validator: {}, ruleName: {}",
                    phase, validator.getClass().getSimpleName(), result.violations().getFirst().ruleName());
            if (this.properties.strictMode()) return result;
        }
        if (!aggregatedViolations.isEmpty()) {
            return GuardrailResult.failure(aggregatedViolations, this.properties.violationMessage(phase));
        }
        return GuardrailResult.success(text);
    }

    private GuardrailResult runValidator(GuardrailValidator validator, String text, ValidationPhase phase) {
        try {
            return validator.validate(text, phase);
        } catch (RuntimeException | StackOverflowError ex) {
            log.error("[GUARDRAIL] [VALIDATOR-ERROR] phase: {}, validator: {}, error: {}",
                    phase, validator.getClass().getSimpleName(), ex.getClass().getSimpleName());
            String msg = this.properties.violationMessage(phase);
            return GuardrailResult.failure(List.of(new GuardrailViolation
                    (phase, "VALIDATOR_ERROR", validator.getClass().getSimpleName(), msg)), msg);
        }
    }

    /**
     * Throws whenever the input is invalid (independent of strictMode, so it can never fail open).
     */
    @Override
    public void validateInput(String prompt) {
        GuardrailResult result = evaluate(prompt, ValidationPhase.INPUT);
        if (!result.valid()) {
            throw new GuardrailViolationException
                    (ValidationPhase.INPUT, result.violations(), result.getFirstViolationMessage());
        }
    }

    /**
     * Throws whenever the output is invalid (independent of strictMode).
     */
    @Override
    public void validateOutput(String response) {
        GuardrailResult result = evaluate(response, ValidationPhase.OUTPUT);
        if (!result.valid()) {
            throw new GuardrailViolationException
                    (ValidationPhase.OUTPUT, result.violations(), result.getFirstViolationMessage());
        }
    }
}