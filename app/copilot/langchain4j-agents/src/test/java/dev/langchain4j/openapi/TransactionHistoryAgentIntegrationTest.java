package dev.langchain4j.openapi;

import com.microsoft.openai.samples.assistant.langchain4j.agent.openapi.TransactionHistoryAgent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;

import java.util.ArrayList;

public class TransactionHistoryAgentIntegrationTest {

    public static void main(String[] args) throws Exception {

        //Azure Open AI Chat Model
        var azureOpenAiChatModel = AzureOpenAiChatModel.builder()
                .apiKey(System.getenv("AZURE_OPENAI_KEY"))
                .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
                .deploymentName(System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME"))
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        var transactionHistoryAgent = new TransactionHistoryAgent(azureOpenAiChatModel,
                "bob.user@contoso.com",
                "http://localhost:8090",
                "http://localhost:8070");

        var chatHistory = new ArrayList<ChatMessage>();


        chatHistory.add(UserMessage.from("When was las time I've paid contoso?"));
        transactionHistoryAgent.invoke(chatHistory);
        System.out.println(chatHistory.get(chatHistory.size()-1));


    }
}
