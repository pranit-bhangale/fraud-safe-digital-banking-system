package com.banking.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class AccountPrompt {

    @McpPrompt(name = "account-summary",
            description = "Summarize an account: balance, status and recent activity")
    public McpSchema.GetPromptResult accountSummary(
            @McpArg(name = "accountId", description = "Account identifier", required = true) String accountId) {
        return PromptSupport.of("Account summary", """
                Task: give the customer a clear summary of account %s.
                Steps:
                1. Fetch the account details and current balance.
                2. Fetch the 5 most recent transactions.
                3. Report: account type, status, available vs. current balance, and recent activity.
                Format: short paragraph, then a bullet list of the recent transactions.
                """.formatted(PromptSupport.clean(accountId, "unknown")));
    }

    @McpPrompt(name = "account-issue-triage",
            description = "Diagnose why an account is blocked, restricted or showing unexpected status")
    public McpSchema.GetPromptResult accountIssue(
            @McpArg(name = "accountId", description = "Account identifier", required = true) String accountId,
            @McpArg(name = "issue", description = "Customer's description of the problem", required = false) String issue) {
        return PromptSupport.of("Account issue triage", """
                Task: investigate a problem with account %s.
                Reported issue: %s
                Steps: check account status and restrictions, then explain the likely cause and the
                next step for the customer. If it needs a human agent, say so and summarize the case.
                """.formatted(PromptSupport.clean(accountId, "unknown"),
                PromptSupport.clean(issue, "not specified")));
    }
}