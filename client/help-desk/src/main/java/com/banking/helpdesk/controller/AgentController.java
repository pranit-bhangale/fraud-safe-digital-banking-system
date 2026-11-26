package com.banking.helpdesk.controller;

import com.banking.helpdesk.chat.AiService;
import com.banking.helpdesk.dto.ChatRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
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

    @PostMapping(value = "/chat", version = "v1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody @Valid ChatRequest request) {
        final int promptLen = request.prompt() != null ? request.prompt().length() : 0;
        log.info("[AI-AGENT] [INCOMING-REQUEST] conversationId: {}, promptLength: {}", request.conversationId(), promptLen);
        return this.aiService.askAiAgent(request.prompt(), request.conversationId());
    }

    /**
     * Lightweight health and probe endpoint for UI connection verification.
     */
    @GetMapping(value = {"/chat", "/health"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Praya AI Agent"));
    }
}
