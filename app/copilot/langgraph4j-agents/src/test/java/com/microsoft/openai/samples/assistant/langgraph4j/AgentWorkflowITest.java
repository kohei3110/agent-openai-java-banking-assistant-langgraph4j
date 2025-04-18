package com.microsoft.openai.samples.assistant.langgraph4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.GraphRepresentation;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AgentWorkflowITest {
    private static final Logger log = LoggerFactory.getLogger("Tests");

    @Test
    public void generateGraphRepresentation() throws Exception {
        var workflow = new AgentWorkflowBuilder().build();

        var result = workflow.getGraph( GraphRepresentation.Type.MERMAID, "Banking Assistant", false);

        System.out.println( result.content() );

    }

    @Test
    public void testWorkflow01() throws Exception {

        var workflow = new AgentWorkflowBuilder().build();

        var userRequest = "i need the infos from my account";

        log.info( "\nrequest by User:\n{}", userRequest );

        var state = workflow.invoke( Map.of( "messages", UserMessage.from( userRequest ) ));


        assertTrue( state.isPresent() );
        assertTrue( state.get().lastMessage().isPresent() );

        log.info( "\nresponse to User:\n{}",
                state.get().lastMessage()
                        .map(AiMessage.class::cast)
                        .map(AiMessage::text)
                        .orElseThrow());

    }
}
