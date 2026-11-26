package com.pranit.praya.guardrail.model;

/**
 * Details of an individual guardrail rule violation.
 */
public record GuardrailViolation(
        ValidationPhase phase,
        String ruleName,
        String matchedTarget,
        String message
) {

    /**
     * Accessor alias for {@link #ruleName()} to support ruleType queries.
     *
     * @return rule name identifier
     */
    public String ruleType() {
        return this.ruleName;
    }
}
