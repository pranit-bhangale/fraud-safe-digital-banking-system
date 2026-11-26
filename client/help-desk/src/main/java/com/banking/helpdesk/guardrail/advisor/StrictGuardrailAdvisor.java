package com.pranit.praya.guardrail.advisor;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.ValidationPhase;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import com.pranit.praya.guardrail.service.GuardrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI advisor enforcing guardrails. Register TWO instances (see GuardrailConfiguration):
 * <ul>
 *   <li>{@link Stage#INPUT}: outermost. Blocked requests never reach chat memory, tools or the model.</li>
 *   <li>{@link Stage#OUTPUT}: innermost (closest to the model). A blocked answer is replaced by the fallback message
 *       BEFORE the memory advisor stores it, so leaked or unsafe text never re-enters later prompts.</li>
 * </ul>
 * Violations always produce a normal assistant message (no exception, no stack trace, no rule details to the client).
 * Streaming output is fully buffered and validated before anything is sent, so partial leaks are impossible.
 */
@Slf4j
@RequiredArgsConstructor
public class StrictGuardrailAdvisor implements CallAdvisor, StreamAdvisor, Ordered {

    private final GuardrailService guardrailService;
    private final GuardrailProperties properties;
    private final Stage stage;
    private final int order;

    @Override
    public @NonNull ChatClientResponse adviseCall(@NonNull ChatClientRequest request, @NonNull CallAdvisorChain chain) {
        if (!properties.enabled()) return chain.nextCall(request);
        if (stage == Stage.INPUT) {
            GuardrailResult input = guardrailService.evaluate(extractUserText(request), ValidationPhase.INPUT);
            if (!input.valid()) return block(request, input, ValidationPhase.INPUT);
            return chain.nextCall(request);
        }
        ChatClientResponse response = chain.nextCall(request);
        GuardrailResult output = guardrailService.evaluate(extractResponseText(response), ValidationPhase.OUTPUT);
        return output.valid() ? response : block(request, output, ValidationPhase.OUTPUT);
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, @NonNull StreamAdvisorChain chain) {
        if (!properties.enabled()) return chain.nextStream(request);
        if (stage == Stage.INPUT) {
            GuardrailResult input = guardrailService.evaluate(extractUserText(request), ValidationPhase.INPUT);
            if (!input.valid()) return Flux.just(block(request, input, ValidationPhase.INPUT));
        }
        return chain.nextStream(request);
    }

    private ChatClientResponse block(ChatClientRequest request, GuardrailResult result, ValidationPhase phase) {
        String rule = result.violations().isEmpty() ? "UNKNOWN" : result.violations().getFirst().ruleName();
        log.warn("[GUARDRAIL] [{}-BLOCKED] ruleName: {}", phase, rule); // never log user/model text
        return createFallbackResponse(request, result.getFirstViolationMessage());
    }

    private ChatClientResponse createFallbackResponse(ChatClientRequest request, String message) {
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(message))))
                        .build())
                .context(Map.copyOf(request.context()))
                .build();
    }

    private String extractUserText(ChatClientRequest request) {
        UserMessage userMessage = request.prompt().getUserMessage();
        return userMessage.getText() != null ? userMessage.getText() : "";
    }

    private String extractResponseText(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Generation generation : response.chatResponse().getResults()) {
            String text = generation.getOutput().getText();
            if (text != null) sb.append(text);
        }
        return sb.toString();
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public @NonNull String getName() {
        return getClass().getSimpleName() + "-" + stage;
    }

    public enum Stage {INPUT, OUTPUT}
}