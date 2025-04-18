package com.microsoft.openai.samples.assistant.langgraph4j;

import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.studio.springboot.AbstractLangGraphStudioConfig;
import org.bsc.langgraph4j.studio.springboot.LangGraphFlow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

public class AgentWorkflowStudio {

    @SpringBootApplication
    public static class LangGraphStudioApplication {

        public static void main(String[] args) {

            SpringApplication.run(LangGraphStudioApplication.class, args);
        }

    }

    @Configuration
    public static class LangGraphStudioSampleConfig extends AbstractLangGraphStudioConfig {

        final LangGraphFlow flow;

        public LangGraphStudioSampleConfig() throws GraphStateException {
            this.flow = agentWorkflow();
        }

        private LangGraphFlow agentWorkflow() throws GraphStateException {

            var workflow = new AgentWorkflowBuilder().graph();

            return  LangGraphFlow.builder()
                    .title("LangGraph Studio (Banking Assistant)")
                    .addInputStringArg( "messages", true, v -> UserMessage.from( Objects.toString(v) ) )
                    .stateGraph( workflow )
                    .compileConfig( CompileConfig.builder()
                            .checkpointSaver( new MemorySaver() )
                            .build())
                    .build();

        }

        @Override
        public LangGraphFlow getFlow() {
            return this.flow;
        }
    }

}
