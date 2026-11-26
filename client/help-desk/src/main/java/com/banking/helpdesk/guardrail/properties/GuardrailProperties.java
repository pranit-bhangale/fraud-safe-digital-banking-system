package com.pranit.praya.guardrail.properties;

import com.pranit.praya.guardrail.model.ValidationPhase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Strongly typed configuration for AI input and output guardrails.
 * Secure by default: if a property is missing (or the prefix is mistyped), guardrails stay ON.
 */
@ConfigurationProperties(prefix = "praya.ai.guardrail")
public record GuardrailProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("true") boolean strictMode,
        @DefaultValue("1000") int maxInputLength,
        @DefaultValue("250") long regexTimeoutMillis,
        List<String> blockedWords,
        List<String> inputPatterns,
        List<String> outputPatterns,
        String inputViolationMessage,
        String outputViolationMessage,
        Scope scope
) {

    public GuardrailProperties {
        if (maxInputLength <= 0) maxInputLength = 1000;
        if (regexTimeoutMillis <= 0) regexTimeoutMillis = 250;
        if (blockedWords == null) blockedWords = List.of();
        if (inputPatterns == null) inputPatterns = List.of();
        if (outputPatterns == null) outputPatterns = List.of();
        if (inputViolationMessage == null || inputViolationMessage.isBlank()) {
            inputViolationMessage = "Input security validation failed: Request violates safety policies.";
        }
        if (outputViolationMessage == null || outputViolationMessage.isBlank()) {
            outputViolationMessage = "Output security validation failed: Response withheld due to safety policies.";
        }
        if (scope == null) scope = Scope.disabled();
    }

    public String violationMessage(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT ? inputViolationMessage : outputViolationMessage;
    }

    /**
     * @param enabled             turn the scope check on/off
     * @param requireTopicKeyword if true, a message longer than followUpMaxWords must contain an allowed topic
     * @param followUpMaxWords    short messages ("tell me more") skip the keyword requirement
     * @param allowedTopics       topic words (matched at word start, so "project" matches "projects")
     * @param smalltalkPatterns   whole-message greetings / thanks that are always allowed
     * @param offTopicPatterns    clearly unrelated requests that are always rejected
     * @param message             friendly message returned when a question is out of scope
     */
    public record Scope(
            boolean enabled,
            boolean requireTopicKeyword,
            int followUpMaxWords,
            List<String> allowedTopics,
            List<String> smalltalkPatterns,
            List<String> offTopicPatterns,
            String message
    ) {
        public Scope {
            if (followUpMaxWords <= 0) followUpMaxWords = 6;
            if (allowedTopics == null) allowedTopics = List.of();
            if (smalltalkPatterns == null) smalltalkPatterns = List.of();
            if (offTopicPatterns == null) offTopicPatterns = List.of();
            if (message == null || message.isBlank()) {
                message = "I can help with questions about my career, skills, projects, "
                        + "blogs, certifications and how to contact me.";
            }
        }

        public static Scope disabled() {
            return new Scope(false, false, 6, List.of(), List.of(), List.of(), null);
        }
    }
}