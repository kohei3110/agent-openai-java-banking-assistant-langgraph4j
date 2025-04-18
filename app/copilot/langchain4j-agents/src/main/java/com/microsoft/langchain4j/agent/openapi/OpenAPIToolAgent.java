package com.microsoft.langchain4j.agent.openapi;

import com.microsoft.langchain4j.agent.AbstractReActAgent;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.openapi.OpenAPIToolsImporter;
import dev.langchain4j.service.tool.ToolExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class OpenAPIToolAgent extends AbstractReActAgent {

    protected  List<ToolSpecification> toolSpecifications;
    protected  Map<String, ToolExecutor> toolExecutorMap;

    protected OpenAPIToolAgent(ChatLanguageModel chatModel, List<OpenAPIImporterMetadata> metadataList) {
        super(chatModel);
        this.toolSpecifications = new ArrayList<>();
        this.toolExecutorMap = new HashMap<>();

        metadataList.forEach(metadata -> {
            OpenAPIToolsImporter importer = OpenAPIToolsImporter.builder()
                .withToolName(metadata.toolName())
                .withSpecPath(metadata.openApiFileName())
                .withServerUrl(metadata.openApiUrl())
                .build();

            toolSpecifications.addAll(importer.getToolSpecifications());
            importer.getToolSpecifications().forEach(spec -> 
                toolExecutorMap.put(spec.name(), importer.getToolExecutor(spec.name())));
        });

    }

    @Override
    protected List<ToolSpecification> getToolSpecifications() {
        return toolSpecifications;
    }

    @Override
    protected ToolExecutor getToolExecutor(String toolName) {
        ToolExecutor executor = toolExecutorMap.get(toolName);
        if (executor == null) {
            throw new IllegalArgumentException("No tool executor found for tool name: " + toolName);
        }
        return executor;
    }

}