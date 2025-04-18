// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.assistant.controller;


import com.microsoft.openai.samples.assistant.langchain4j.agent.SupervisorRoutingAgent;
import com.microsoft.openai.samples.assistant.langgraph4j.SupervisorAgent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "agent.strategy", havingValue = "langchain4j", matchIfMissing = true)
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private final SupervisorRoutingAgent supervisorRoutingAgent;

    public ChatController(SupervisorRoutingAgent supervisorRoutingAgent){
        this.supervisorRoutingAgent = supervisorRoutingAgent;
    }


    @PostMapping(value = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> openAIAsk(@RequestBody ChatAppRequest chatRequest) {
        if (chatRequest.stream()) {
            LOGGER.warn(
                    "Requested a content-type of application/json however also requested streaming."
                            + " Please use a content-type of application/ndjson");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested a content-type of application/json however also requested streaming."
                            + " Please use a content-type of application/ndjson");
        }

        if (chatRequest.messages() == null || chatRequest.messages().isEmpty()) {
            LOGGER.warn("history cannot be null in Chat request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        List<ChatMessage> chatHistory = convertToLangchain4j(chatRequest);


        LOGGER.debug("Processing chat conversation..", chatHistory.get(chatHistory.size()-1));

        List<ChatMessage> agentsResponse = supervisorRoutingAgent.invoke(chatHistory);

        AiMessage generatedResponse = (AiMessage) agentsResponse.get(agentsResponse.size()-1);
        return ResponseEntity.ok(
                ChatResponse.buildChatResponse(generatedResponse, UUID.randomUUID().toString()));
    }

    private List<ChatMessage> convertToLangchain4j(ChatAppRequest chatAppRequest) {
       List<ChatMessage> chatHistory = new ArrayList<>();
         chatAppRequest.messages().forEach(
               historyChat -> {
                   if("user".equals(historyChat.role())) {
                     if(historyChat.attachments() == null || historyChat.attachments().isEmpty())
                         chatHistory.add(UserMessage.from(historyChat.content()));
                     else
                         chatHistory.add(UserMessage.from(historyChat.content() + " " + historyChat.attachments().toString()));
                   }
                   if("assistant".equals(historyChat.role()))
                   chatHistory.add(AiMessage.from(historyChat.content()));
               });
       return chatHistory;

    }
}
