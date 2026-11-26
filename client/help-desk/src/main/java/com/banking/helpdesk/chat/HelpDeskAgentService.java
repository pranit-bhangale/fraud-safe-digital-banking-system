package com.banking.helpdesk.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.service.AgentCacheService;
import com.pranit.praya.chat.ChatEventStream;
import com.pranit.praya.constant.StreamEventConstant;
import com.pranit.praya.guardrail.exception.GuardrailViolationException;
import com.pranit.praya.guardrail.service.GuardrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Main AI Agent service for Praya (Pranit's Personal Assistant).
 * Integrates strict input/output guardrails, caching, and multi-tool orchestration.
 */
@Slf4j
@Service
public class PrayaAiService implements AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AgentCacheService agentCacheService;
    private final GuardrailService guardrailService;

    public PrayaAiService(
            ChatClient chatClient,
            ObjectMapper objectMapper,
            AgentCacheService agentCacheService,
            GuardrailService guardrailService
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.agentCacheService = agentCacheService;
        this.guardrailService = guardrailService;
    }

    @Override
    public Flux<ServerSentEvent<String>> askAiAgent(final String prompt, final String conversationId) {
        final long startTime = System.currentTimeMillis();
        final ChatEventStream events = new ChatEventStream(this.objectMapper);
        try {
            this.guardrailService.validateInput(prompt);
        } catch (GuardrailViolationException ex) {
            log.warn("[AI-AGENT] [INPUT-BLOCKED] conversationId: {}, reason: {}", conversationId, ex.getMessage());
            return Flux.just(
                    ChatEventStream.sse(StreamEventConstant.EVENT_ERROR, ex.getMessage()),
                    ChatEventStream.sse(StreamEventConstant.EVENT_DONE, "")
            );
        }
        final String cachedAnswer = this.agentCacheService.getCachedAnswer(prompt);
        if (cachedAnswer != null && !cachedAnswer.isBlank()) {
            final long latencyMs = System.currentTimeMillis() - startTime;
            log.info("[AI-AGENT] [CACHE-HIT] conversationId: {}, cachedCharacters: {}, latencyMs: {}",
                    conversationId, cachedAnswer.length(), latencyMs);
            Flux<ServerSentEvent<String>> cachedStream =
                    Flux.fromArray(cachedAnswer.split("(?<=\\s)|(?<=\\n)")).map(events::token);
            return events.mergeWith(cachedStream);
        }
        log.info("[AI-AGENT] [CACHE-MISS] conversationId: {}, initiating model inference", conversationId);
        final StringBuilder answerAccumulator = new StringBuilder();
        final Flux<ServerSentEvent<String>> answerTokens = this.chatClient
                .prompt()
                .advisors(events.observer())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(prompt)
                .stream()
                .content()
                .doOnNext(answerAccumulator::append)
                .doOnComplete(() -> {
                    final String fullResponse = answerAccumulator.toString();
                    final long durationMs = System.currentTimeMillis() - startTime;
                    if (!fullResponse.isBlank()) {
                        try {
                            this.guardrailService.validateOutput(fullResponse);
                            this.agentCacheService.putAnswer(prompt, fullResponse);
                            log.info("[AI-AGENT] [RESPONSE-COMPLETE] conversationId: {}, generatedCharacters: {}, durationMs: {}",
                                    conversationId, fullResponse.length(), durationMs);
                        } catch (GuardrailViolationException ex) {
                            log.warn("[AI-AGENT] [OUTPUT-BLOCKED] conversationId: {}, output validation failed: {}",
                                    conversationId, ex.getMessage());
                        }
                    }
                })
                .map(events::token);
        return events.mergeWith(answerTokens);
    }
}
