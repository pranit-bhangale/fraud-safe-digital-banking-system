package com.banking.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class TransactionPrompt {

    @McpPrompt(name = "transaction-history",
            description = "Explain recent transactions for an account")
    public McpSchema.GetPromptResult history(
            @McpArg(name = "accountId", required = true) String accountId,
            @McpArg(name = "days", description = "Look-back window in days (default 30)", required = false) String days) {
        return PromptSupport.of("Transaction history", """
                Task: list and summarize transactions for account %s over the last %s days.
                Group by category if available, show totals for money in and money out,
                and highlight the largest 3 transactions. Do not exceed what the tools return.
                """.formatted(PromptSupport.clean(accountId, "unknown"), PromptSupport.clean(days, "30")));
    }

    @McpPrompt(name = "dispute-transaction",
            description = "Help a customer review a transaction they don't recognize")
    public McpSchema.GetPromptResult dispute(
            @McpArg(name = "transactionId", required = true) String transactionId) {
        return PromptSupport.of("Dispute a transaction", """
                Task: help the customer with transaction %s they do not recognize.
                Steps:
                1. Fetch the transaction (merchant, amount, date, status, channel).
                2. Explain what the record shows, without accusing anyone.
                3. If still disputed, collect the reason and tell them the next step.
                Do not promise refunds or outcomes.
                """.formatted(PromptSupport.clean(transactionId, "unknown")));
    }
}