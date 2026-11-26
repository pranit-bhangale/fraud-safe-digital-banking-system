package com.pranit.praya.guardrail.service;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.ValidationPhase;

/**
 * Service contract for orchestrating guardrail evaluation across input and output lifecycle phases.
 */
public interface GuardrailService {

    GuardrailResult evaluate(String text, ValidationPhase phase);

    void validateInput(String prompt);

    void validateOutput(String response);
}
