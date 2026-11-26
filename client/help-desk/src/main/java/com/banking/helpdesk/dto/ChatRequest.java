package com.banking.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ChatRequest(
        @NotBlank(message = "Prompt cannot be blank")
        String prompt,
        String conversationId
) {
}

