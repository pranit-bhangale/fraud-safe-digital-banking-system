package com.banking.helpdesk.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final AiService aiService;

    /**
     * Public chat endpoint secured by a two-tier stacked rate-limiting defense:
     * 1. Global IP Shield (Coarse-Grained): Protects the backend against flood attacks and scraping (max 20 req/60s).
     * 2. Conversation Pacer (Fine-Grained): Prevents AI session spamming within a single conversation (max 5 req/15s).
     */
    @RateLimits({
            @RateLimit(
                    name = "agent-chat-ip",
                    limit = 20,
                    window = 60,
                    cooldown = 30,
                    key = RateLimit.KeyType.IP,
                    failOpen = true
            ),
            @RateLimit(
                    name = "agent-chat-conversation",
                    limit = 5,
                    window = 15,
                    cooldown = 10,
                    key = RateLimit.KeyType.EXPRESSION,
                    keyExpression = "#request.conversationId()",
                    failOpen = true
            )
    })
    @PostMapping(value = "/chat", version = "v1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody @Valid ChatRequest request) {
        final String conversationId = StringUtils.hasText(request.conversationId()) ? request.conversationId() : AiConstant.DEFAULT_CONVERSATION_ID;
        final int promptLen = request.prompt() != null ? request.prompt().length() : 0;
        log.info("[AI-AGENT] [INCOMING-REQUEST] conversationId: {}, promptLength: {}", conversationId, promptLen);
        return this.aiService.askAiAgent(request.prompt(), conversationId);
    }

    /**
     * Lightweight health and probe endpoint for UI connection verification.
     */
    @GetMapping(value = {"/chat", "/health"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Praya AI Agent"));
    }
}
