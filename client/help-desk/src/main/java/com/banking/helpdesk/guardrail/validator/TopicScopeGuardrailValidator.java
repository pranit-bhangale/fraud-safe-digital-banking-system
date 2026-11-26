package com.banking.helpdesk.guardrail.validator;

import com.banking.helpdesk.guardrail.model.GuardrailResult;
import com.banking.helpdesk.guardrail.model.GuardrailViolation;
import com.banking.helpdesk.guardrail.model.ValidationPhase;
import com.banking.helpdesk.guardrail.properties.GuardrailProperties;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * INPUT-only topic scope check for a portfolio assistant.
 * Optimized with a unified single-pass regex for all allowed topics.
 * Supports individual YAML items, grouped lines, or single comma-separated sentences.
 */
@Component
public class TopicScopeGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<Pattern> smalltalk;
    private final List<Pattern> offTopic;
    private final Pattern unifiedAllowedTopicsPattern;

    public TopicScopeGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        GuardrailProperties.Scope scope = properties.scope();
        this.smalltalk = scope.smalltalkPatterns().stream()
                .filter(StringUtils::hasText).map(p ->
                        Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).toList();
        this.offTopic = scope.offTopicPatterns().stream()
                .filter(StringUtils::hasText).map(p ->
                        Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).toList();

        List<String> rawTopics = scope.allowedTopics() != null ? scope.allowedTopics() : List.of();
        List<String> flattenedTopics = rawTopics.stream()
                .filter(StringUtils::hasText)
                .flatMap(entry -> Arrays.stream(entry.split("[,;]+")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        if (!flattenedTopics.isEmpty()) {
            String alternation = flattenedTopics.stream()
                    .map(TopicScopeGuardrailValidator::compileTopicRegex)
                    .collect(Collectors.joining("|"));
            this.unifiedAllowedTopicsPattern = Pattern.compile("(?<![\\p{L}\\p{N}_])(" + alternation + ")",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } else {
            this.unifiedAllowedTopicsPattern = null;
        }
    }

    private static String compileTopicRegex(String topic) {
        String[] words = topic.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append("\\s+");
            sb.append(Pattern.quote(words[i]));
        }
        return sb.toString();
    }

    private static int wordCount(String text) {
        return text.trim().split("\\s+").length;
    }

    private static GuardrailResult fail(ValidationPhase phase, String rule, String target, String message) {
        return GuardrailResult.failure(List.of(new GuardrailViolation(phase, rule, target, message)), message);
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT && properties.scope().enabled();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text)) return GuardrailResult.success(text);
        GuardrailProperties.Scope scope = properties.scope();
        for (Pattern p : smalltalk) {
            if (p.matcher(text).find()) return GuardrailResult.success(text);
        }
        for (Pattern p : offTopic) {
            if (p.matcher(text).find()) return fail(phase, "OFF_TOPIC", p.pattern(), scope.message());
        }
        if (scope.requireTopicKeyword() && wordCount(text) > scope.followUpMaxWords()) {
            boolean onTopic = this.unifiedAllowedTopicsPattern != null && this.unifiedAllowedTopicsPattern.matcher(text).find();
            if (!onTopic) return fail(phase, "NO_ALLOWED_TOPIC", "allowed-topics", scope.message());
        }
        return GuardrailResult.success(text);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}