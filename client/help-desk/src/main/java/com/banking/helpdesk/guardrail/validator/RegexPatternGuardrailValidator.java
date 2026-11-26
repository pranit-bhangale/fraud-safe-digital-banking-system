package com.banking.helpdesk.guardrail.validator;

import com.banking.helpdesk.guardrail.model.GuardrailResult;
import com.banking.helpdesk.guardrail.model.GuardrailViolation;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import com.banking.helpdesk.guardrail.properties.GuardrailProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Pre-compiled regex evaluation (prompt injection, secret leaks, prompt leakage).
 * Every match runs under a time budget. A pattern that exceeds it (catastrophic backtracking)
 * is treated as a violation (fail-closed), so a bad pattern can never hang a request thread.
 */
@Component
public class RegexPatternGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<Pattern> compiledInputPatterns;
    private final List<Pattern> compiledOutputPatterns;

    public RegexPatternGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        this.compiledInputPatterns = compileAll(properties.inputPatterns());
        this.compiledOutputPatterns = compileAll(properties.outputPatterns());
    }

    private static List<Pattern> compileAll(List<String> raw) {
        return raw == null ? List.of() : raw.stream()
                .filter(StringUtils::hasText)
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE))
                .toList();
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT
                ? !this.compiledInputPatterns.isEmpty()
                : !this.compiledOutputPatterns.isEmpty();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text)) return GuardrailResult.success(text);
        final List<Pattern> targetPatterns = (phase == ValidationPhase.INPUT)
                ? this.compiledInputPatterns : this.compiledOutputPatterns;
        if (targetPatterns.isEmpty()) return GuardrailResult.success(text);
        final List<GuardrailViolation> violations = new ArrayList<>();
        final String fallbackMessage = this.properties.violationMessage(phase);
        final long budgetNanos = TimeUnit.MILLISECONDS.toNanos(this.properties.regexTimeoutMillis());

        for (Pattern pattern : targetPatterns) {
            boolean found;
            try {
                found = pattern.matcher(new DeadlineCharSequence(text, System.nanoTime() + budgetNanos)).find();
            } catch (RegexTimeoutException ex) {
                violations.add(new GuardrailViolation(phase, "REGEX_TIMEOUT", pattern.pattern(), fallbackMessage));
                return GuardrailResult.failure(violations, fallbackMessage); // fail-closed immediately
            }
            if (found) {
                violations.add(new GuardrailViolation(phase, "REGEX_PATTERN_MATCH", pattern.pattern(), fallbackMessage));
                if (this.properties.strictMode()) return GuardrailResult.failure(violations, fallbackMessage);
            }
        }
        if (!violations.isEmpty()) return GuardrailResult.failure(violations, fallbackMessage);
        return GuardrailResult.success(text);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }


    private static final class RegexTimeoutException extends RuntimeException {
        RegexTimeoutException() {
            super("regex time budget exceeded", null, false, false);
        }
    }

    /**
     * CharSequence that aborts matching once the deadline has passed (checked every 1024 reads).
     */
    private static final class DeadlineCharSequence implements CharSequence {
        private final CharSequence delegate;
        private final long deadlineNanos;
        private int reads;

        DeadlineCharSequence(CharSequence delegate, long deadlineNanos) {
            this.delegate = delegate;
            this.deadlineNanos = deadlineNanos;
        }

        @Override
        public char charAt(int index) {
            if ((++reads & 0x3FF) == 0 && System.nanoTime() > deadlineNanos) throw new RegexTimeoutException();
            return delegate.charAt(index);
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public @NonNull CharSequence subSequence(int start, int end) {
            return new DeadlineCharSequence(delegate.subSequence(start, end), deadlineNanos);
        }

        @Override
        public @NonNull String toString() {
            return delegate.toString();
        }
    }
}