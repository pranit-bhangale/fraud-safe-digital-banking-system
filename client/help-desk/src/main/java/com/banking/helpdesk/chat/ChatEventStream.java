package com.pranit.praya.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.constant.StreamEventConstant;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Per-request bundle of SSE streaming components for a chat turn.
 */
public class ChatEventStream {

    private final ObjectMapper json;
    private final Sinks.Many<ServerSentEvent<String>> toolEvents =
            Sinks.many().multicast().onBackpressureBuffer();
    private final ToolCallObservingAdvisor observer;

    public ChatEventStream(ObjectMapper json) {
        this.json = json;
        this.observer = new ToolCallObservingAdvisor(event -> {
            try {
                this.toolEvents.tryEmitNext(sse(StreamEventConstant.EVENT_TOOL, this.json.writeValueAsString(event)));
            } catch (JsonProcessingException e) {
                this.toolEvents.tryEmitNext(sse(StreamEventConstant.EVENT_ERROR, "Failed to serialize tool event: " + e.getMessage()));
            }
        });
    }

    public static ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    public ToolCallObservingAdvisor observer() {
        return this.observer;
    }

    public ServerSentEvent<String> token(String token) {
        try {
            return sse(StreamEventConstant.EVENT_TOKEN, this.json.writeValueAsString(token));
        } catch (JsonProcessingException e) {
            return sse(StreamEventConstant.EVENT_TOKEN, "\"" + token.replace("\"", "\\\"") + "\"");
        }
    }

    public Flux<ServerSentEvent<String>> mergeWith(Flux<ServerSentEvent<String>> tokens) {
        return Flux.merge(tokens.doFinally(_ ->
                        this.toolEvents.tryEmitComplete()), this.toolEvents.asFlux())
                .onErrorResume(e -> Flux.just(sse(StreamEventConstant.EVENT_ERROR, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())))
                .concatWith(Flux.just(sse(StreamEventConstant.EVENT_DONE, "")));
    }
}
