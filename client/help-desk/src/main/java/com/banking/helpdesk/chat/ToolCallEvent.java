package com.banking.helpdesk.chat;

import lombok.Builder;

/**
 * Event payload emitted to clients whenever an AI tool is invoked or completes.
 */
@Builder
public record ToolCallEvent(
        String id,
        String name,
        Phase phase,
        String responseData
) {
    public enum Phase {
        STARTED,
        COMPLETED
    }
}
