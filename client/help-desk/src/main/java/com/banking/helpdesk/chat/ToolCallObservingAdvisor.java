package com.banking.helpdesk.chat;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Advisor observing every intermediate tool call and completion within Spring AI's tool loop.
 */
@Slf4j
public class ToolCallObservingAdvisor implements CallAdvisor, StreamAdvisor {

    public static final int ORDER = ToolCallingAdvisor.DEFAULT_ORDER + 100;

    private final Consumer<ToolCallEvent> listener;
    private final Set<String> started = ConcurrentHashMap.newKeySet();
    private final Set<String> completed = ConcurrentHashMap.newKeySet();

    public ToolCallObservingAdvisor(Consumer<ToolCallEvent> listener) {
        this.listener = listener;
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, StreamAdvisorChain chain) {
        emitCompletions(request);
        return chain.nextStream(request).doOnNext(this::emitStarts);
    }

    private void emitCompletions(ChatClientRequest request) {
        request.prompt().getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .flatMap(message -> message.getResponses().stream())
                .filter(response -> {
                    String id = StringUtils.hasText(response.id()) ? response.id() : response.name();
                    return (this.started.contains(id)
                            || this.started.contains(response.name())
                            || this.started.isEmpty())
                            && this.completed.add(id);
                })
                .forEach(response -> {
                    String id = StringUtils.hasText(response.id()) ? response.id() : response.name();
                    int payloadBytes = response.responseData().length();
                    log.info("[AI-AGENT] [TOOL-COMPLETED] tool: {}, payloadCharacters: {}", response.name(), payloadBytes);
                    this.listener.accept(ToolCallEvent.builder()
                            .id(id)
                            .name(response.name())
                            .phase(ToolCallEvent.Phase.COMPLETED)
                            .responseData(response.responseData())
                            .build());
                });
    }

    private void emitStarts(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) return;
        chatResponse.getResults()
                .stream()
                .flatMap(generation -> generation.getOutput().getToolCalls().stream())
                .filter(toolCall -> StringUtils.hasText(toolCall.name()))
                .filter(toolCall -> {
                    String id = StringUtils.hasText(toolCall.id()) ? toolCall.id() : toolCall.name();
                    return this.started.add(id);
                })
                .forEach(toolCall -> {
                    String id = StringUtils.hasText(toolCall.id()) ? toolCall.id() : toolCall.name();
                    log.info("[AI-AGENT] [TOOL-STARTED] tool: {}", toolCall.name());
                    this.listener.accept(ToolCallEvent.builder()
                            .id(id)
                            .name(toolCall.name())
                            .phase(ToolCallEvent.Phase.STARTED)
                            .build());
                });
    }

    @Override
    public @NonNull ChatClientResponse adviseCall(@NonNull ChatClientRequest request, CallAdvisorChain chain) {
        emitCompletions(request);
        ChatClientResponse response = chain.nextCall(request);
        emitStarts(response);
        return response;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public @NonNull String getName() {
        return getClass().getSimpleName();
    }
}
