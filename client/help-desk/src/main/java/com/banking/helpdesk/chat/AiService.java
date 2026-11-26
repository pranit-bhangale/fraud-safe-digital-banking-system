package com.pranit.praya.chat;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * High-level contract for the Praya AI chat streaming assistant.
 */
public interface AiService {

    Flux<ServerSentEvent<String>> askAiAgent(String prompt, String conversationId);
}
