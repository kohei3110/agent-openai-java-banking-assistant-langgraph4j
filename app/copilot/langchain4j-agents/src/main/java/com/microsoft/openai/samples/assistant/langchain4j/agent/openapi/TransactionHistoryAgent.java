package com.microsoft.openai.samples.assistant.langchain4j.agent.openapi;

import com.microsoft.langchain4j.agent.AgentMetadata;
import com.microsoft.langchain4j.agent.openapi.OpenAPIImporterMetadata;
import com.microsoft.langchain4j.agent.openapi.OpenAPIToolAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.List;
import java.util.Map;

public class TransactionHistoryAgent extends OpenAPIToolAgent {

    private final Prompt agentPrompt;

    private static final String TRANSACTION_AGENT_SYSTEM_MESSAGE = """
        you are a personal financial advisor who help the user with their recurrent bill payments. To search about the payments history you need to know the payee name and the account id.
        If the user doesn't provide the payee name, search the last 10 transactions order by date.
        If the user want to search last transactions for a specific payee, ask to provide the payee name.
        Use html list or table to display the transaction information.
        Always use the below logged user details to retrieve account info:
        '{{loggedUserName}}'
        Current timestamp:
        '{{currentDateTime}}'
        """;

    public TransactionHistoryAgent(ChatLanguageModel chatModel, String loggedUserName, String transactionAPIUrl, String accountAPIUrl) {
        super(chatModel, List.of(
            new OpenAPIImporterMetadata("transaction-api", "transaction-history.yaml", transactionAPIUrl),
            new OpenAPIImporterMetadata("account-api", "account.yaml", accountAPIUrl)
        ));

        if (loggedUserName == null || loggedUserName.isEmpty()) {
            throw new IllegalArgumentException("loggedUserName cannot be null or empty");
        }

        PromptTemplate promptTemplate = PromptTemplate.from(TRANSACTION_AGENT_SYSTEM_MESSAGE);
        var datetimeIso8601 = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC")).toInstant().toString();

        this.agentPrompt = promptTemplate.apply(Map.of(
            "loggedUserName", loggedUserName,
            "currentDateTime", datetimeIso8601
        ));
    }

    @Override
    public String getName() {
        return "TransactionHistoryAgent";
    }

    @Override
    public AgentMetadata getMetadata() {
        return new AgentMetadata(
            "Personal financial advisor for managing transaction history.",
            List.of("SearchTransactions", "GetTransactionDetails", "CheckTransactionHistory")
        );
    }

    @Override
    protected String getSystemMessage() {
        return agentPrompt.text();
    }
}
