package com.microsoft.openai.samples.assistant.langgraph4j;

import dev.langchain4j.data.message.AiMessage;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.NodeActionWithConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static org.bsc.langgraph4j.action.AsyncNodeActionWithConfig.node_async;


/**
 * Implements methods for managing a financial advisor node that processes user intents.
 * The class provides functionality to evaluate user messages and determine the appropriate action based on pre-defined
 * intent patterns.
 */
public class SupervisorAgentNode implements NodeActionWithConfig<AgentWorkflowState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisorAgentNode.class);
    final SupervisorAgent agent;

    public static AsyncNodeActionWithConfig<AgentWorkflowState> of(SupervisorAgent agent) {
        return node_async( new SupervisorAgentNode(agent) );
    }

    private SupervisorAgentNode(SupervisorAgent agent) {
        this.agent = Objects.requireNonNull( agent, "agent cannot be null");
    }

    @Override
    public Map<String, Object> apply(AgentWorkflowState state, RunnableConfig config) {

        var messages = agent.invoke(state.messages());

        if (messages.get(0) instanceof AiMessage nextAgentMessage) {
            LOGGER.info("Supervisor Agent handoff to [{}]", nextAgentMessage.text());

            if("none".equalsIgnoreCase(nextAgentMessage.text())){
                LOGGER.info("Gracefully handle clarification.. ");
                AiMessage clarificationMessage = AiMessage.builder().
                                                   text(" I'm not sure about your request. Can you please clarify?")
                                                   .build();

                return Map.of("nextAgent", "none", "messages", clarificationMessage);
            }
            return Map.of("nextAgent", nextAgentMessage.text());
        }

        throw new IllegalArgumentException("Invalid message type from Supervisor: " + messages.get(0).getClass().getName());
    }
}