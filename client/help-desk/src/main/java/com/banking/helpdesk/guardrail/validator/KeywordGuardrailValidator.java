package com.banking.helpdesk.guardrail.validator;

import com.banking.helpdesk.guardrail.model.GuardrailResult;
import com.banking.helpdesk.guardrail.model.GuardrailViolation;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import com.banking.helpdesk.guardrail.properties.GuardrailProperties;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Concrete validator strategy for case-insensitive, whole-word blocked word detection (INPUT only).
 */
@Component
public class KeywordGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<Pattern> blockedPatterns;

    public KeywordGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        this.blockedPatterns = properties.blockedWords() != null
                ? properties.blockedWords()
                .stream()
                .filter(StringUtils::hasText)
                .map(KeywordGuardrailValidator::compilePattern)
                .filter(Objects::nonNull)
                .toList()
                : List.of();
    }

    private static Pattern compilePattern(String rawWord) {
        String trimmed = rawWord.trim();
        if (trimmed.isEmpty()) return null;

        String[] tokens = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();

        char firstChar = tokens[0].charAt(0);
        if (Character.isLetterOrDigit(firstChar) || firstChar == '_') {
            sb.append("(?<![\\p{L}\\p{N}_])");
        }

        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append("\\s+");
            }
            sb.append(Pattern.quote(tokens[i]));
        }

        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (Character.isLetterOrDigit(lastChar) || lastChar == '_') {
            sb.append("(?![\\p{L}\\p{N}_])");
        }

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT && !this.blockedPatterns.isEmpty();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text) || this.blockedPatterns.isEmpty()) return GuardrailResult.success(text);
        final List<GuardrailViolation> violations = new ArrayList<>();
        final String fallbackMessage = phase == ValidationPhase.INPUT
                ? this.properties.inputViolationMessage() : this.properties.outputViolationMessage();
        for (Pattern blockedWord : this.blockedPatterns) {
            if (blockedWord.matcher(text).find()) {
                violations.add(new GuardrailViolation(
                        phase,
                        "KEYWORD_BLOCKED",
                        blockedWord.pattern(),
                        fallbackMessage
                ));
                if (this.properties.strictMode()) return GuardrailResult.failure(violations, fallbackMessage);
            }
        }
        if (!violations.isEmpty()) return GuardrailResult.failure(violations, fallbackMessage);
        return GuardrailResult.success(text);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}