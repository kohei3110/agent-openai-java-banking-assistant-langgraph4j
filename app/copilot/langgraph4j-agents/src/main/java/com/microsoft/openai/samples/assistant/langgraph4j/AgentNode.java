package com.microsoft.openai.samples.assistant.langgraph4j;

import com.microsoft.langchain4j.agent.Agent;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public  class AgentNode<A extends Agent> implements NodeAction<AgentWorkflowState> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentNode.class);

    private final Agent agent;

    public static <A extends Agent> AsyncNodeAction<AgentWorkflowState> of( A agent ) {
        return node_async( new AgentNode<>( agent ));
    }

    public AgentNode( A agent ) {
        this.agent = Objects.requireNonNull( agent, "agent cannot be null");
    }

    @Override
    public Map<String, Object> apply(AgentWorkflowState state) throws Exception {

        var messages = agent.invoke( state.messages() );

        return Map.of( "messages", messages );
    }
}
