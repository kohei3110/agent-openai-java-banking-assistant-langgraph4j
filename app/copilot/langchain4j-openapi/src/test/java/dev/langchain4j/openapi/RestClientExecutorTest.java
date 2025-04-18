package dev.langchain4j.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.http.client.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;


class RestClientExecutorTest {

    private static WireMockServer wireMockServer;
    private static HttpClient httpClient;

    @BeforeAll
    public static void startMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().port(8080));
        wireMockServer.start();

        HttpClientBuilder httpClientBuilder  = HttpClientBuilderLoader.loadHttpClientBuilder();
        httpClient = httpClientBuilder
                .connectTimeout(ofSeconds(15))
                .readTimeout(ofSeconds(60))
                .build();
    }

    @AfterAll
    public static void stopMockServer() {
        wireMockServer.stop();
    }

    @Test
    void testBuildQueryString() {
        // Setup
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        QueryParameter param1 = new QueryParameter();
        param1.setName("param1");
        QueryParameter param2 = new QueryParameter();
        param2.setName("param2");
        operation.addParametersItem(param1);
        operation.addParametersItem(param2);
        pathItem.setGet(operation);

        HttpClient client = null; // Mock or create an instance as needed
        Map<String, List<String>> httpHeaders = new HashMap<>();

        RestClientToolExecutor executor = new RestClientToolExecutor(
                HttpMethod.GET,
                "http://example.com",
                "/path",
                pathItem,
                client,
                httpHeaders,
                operation
        );

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("param1", "value1");
        arguments.put("param2", "value2");

        // Execute
        String queryString = executor.buildQueryString(arguments);

        // Verify
        assertTrue(queryString.contains("param1=value1"));
        assertTrue(queryString.contains("param2=value2"));
    }

    @Test
    void testBuildQueryPath() {
        // Setup
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        PathParameter param1 = new PathParameter();
        param1.setName("param1");
        PathParameter param2 = new PathParameter();
        param2.setName("param2");
        operation.addParametersItem(param1);
        operation.addParametersItem(param2);
        pathItem.setGet(operation);

        HttpClient client = null; // Mock or create an instance as needed
        Map<String, List<String>> httpHeaders = new HashMap<>();

        RestClientToolExecutor executor = new RestClientToolExecutor(
                HttpMethod.GET,
                "http://example.com",
                "/path/{param1}/{param2}",
                pathItem,
                client,
                httpHeaders,
                operation
        );

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("param1", "value1");
        arguments.put("param2", "value2");

        // Execute
        String queryPath = executor.buildQueryPath(arguments);

        // Verify
        assertEquals("/path/value1/value2", queryPath);
    }

    @Test
    void testGetBody() {
        // Setup
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("requestbody", "{\"key1\":\"value1\",\"key2\":\"value2\"}");

        RestClientToolExecutor executor = new RestClientToolExecutor(
                HttpMethod.POST,
                "http://example.com",
                "/path",
                new PathItem(),
                null, // Mock or create an instance as needed
                new HashMap<>(),
                new Operation()
        );

        // Execute
        String body = executor.getBody(arguments);

        // Verify

        ObjectMapper objectMapper = new ObjectMapper();
        try {
        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}",
                     objectMapper.readTree(body).asText());
        } catch (Exception e) {
            fail("Failed to parse JSON");
        }
    }

    @Test
    void testExecuteWithURLParams() {
        // Setup Mock Server
        wireMockServer.stubFor(get(urlPathTemplate("/path/{pathParam1}/{pathParam2}"))
                .withQueryParam("param1", equalTo("value1"))
                .withQueryParam("param2", equalTo("value2"))
                .withPathParam("pathParam1", equalTo("pathValue1"))
                .withPathParam("pathParam2", equalTo("pathValue2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));

        // Setup RestClientToolExecutor
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        QueryParameter param1 = new QueryParameter();
        param1.setName("param1");
        QueryParameter param2 = new QueryParameter();
        param2.setName("param2");
        PathParameter pathParameter1 = new PathParameter();
        pathParameter1.setName("pathParam1");
        PathParameter pathParameter2 = new PathParameter();
        pathParameter2.setName("pathParam2");
        operation.addParametersItem(param1);
        operation.addParametersItem(param2);
        operation.addParametersItem(pathParameter1);
        operation.addParametersItem(pathParameter2);
        pathItem.setGet(operation);

        Map<String, List<String>> httpHeaders = new HashMap<>();

        RestClientToolExecutor executor = new RestClientToolExecutor(
                HttpMethod.GET,
                "http://localhost:8080",
                "/path/{pathParam1}/{pathParam2}",
                pathItem,
                httpClient,
                httpHeaders,
                operation
        );

        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder().arguments(
           """
              {
                      "param1": "value1",
                      "param2": "value2",
                      "pathParam1": "pathValue1",
                      "pathParam2": "pathValue2"
                    }
             """).build();

        // Execute
        String response = executor.execute(toolExecutionRequest, null);

        // Verify
        assertEquals("Success", response);
    }

    @Test
    void testExecuteWithPathParamsAndRequestBody() {
        // Setup Mock Server
        wireMockServer.stubFor(post(urlPathTemplate("/path/{pathParam1}/{pathParam2}"))
                .withRequestBody(equalToJson("{\"key1\":\"value1\",\"key2\":\"value2\"}"))
                .withPathParam("pathParam1", equalTo("pathValue1"))
                .withPathParam("pathParam2", equalTo("pathValue2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));

        // Setup RestClientToolExecutor
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        PathParameter pathParameter1 = new PathParameter();
        pathParameter1.setName("pathParam1");
        PathParameter pathParameter2 = new PathParameter();
        pathParameter2.setName("pathParam2");
        operation.addParametersItem(pathParameter1);
        operation.addParametersItem(pathParameter2);
        pathItem.setPost(operation);

        Map<String, List<String>> httpHeaders = new HashMap<>();

        RestClientToolExecutor executor = new RestClientToolExecutor(
                HttpMethod.POST,
                "http://localhost:8080",
                "/path/{pathParam1}/{pathParam2}",
                pathItem,
                httpClient,
                httpHeaders,
                operation
        );

        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder().arguments(
           """
              {
                      "pathParam1": "pathValue1",
                      "pathParam2": "pathValue2",
                      "requestbody": {"key1":"value1","key2":"value2"}
                    }
             """).build();

        // Execute
        String response = executor.execute(toolExecutionRequest, null);

        // Verify
        assertEquals("Success", response);
    }


}
