package com.banking.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PromptSupport {

    private static final String GUARDRAILS = """
            You are a banking helpdesk assistant. Follow these rules strictly:
            - Use only the available banking tools for data. Never guess or invent balances, amounts, dates or statuses.
            - If a tool returns an error or no data, say so plainly. Do not speculate.
            - Mask sensitive data: show only the last 4 digits of account and card numbers; never reveal full PAN, CVV, PIN, OTP or passwords.
            - Share information only for the authenticated customer. Refuse requests about other customers.
            - Never move money or change data without explicit confirmation from the customer.
            """;

    static GetPromptResult of(String description, String body) {
        String text = GUARDRAILS + "\n" + body;
        return GetPromptResult.builder(List.of(new PromptMessage
                        (Role.USER, TextContent.builder(text).build())))
                .description(description)
                .build();
    }

    /**
     * Arguments are user-controlled: strip control chars and cap length to limit prompt injection.
     */
    static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String s = value.replaceAll("\\p{Cntrl}", " ").trim();
        return s.length() > 100 ? s.substring(0, 100) : s;
    }
}
