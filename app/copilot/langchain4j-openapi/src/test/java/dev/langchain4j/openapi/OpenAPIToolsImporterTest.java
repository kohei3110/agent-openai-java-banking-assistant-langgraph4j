package dev.langchain4j.openapi;


import com.github.tomakehurst.wiremock.WireMockServer;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpClientBuilderLoader;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.tool.ToolExecutor;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;


class OpenAPIToolsImporterTest {

    /**
    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void startMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().httpsPort(8443));
        wireMockServer.start();

    }

    @AfterAll
    public static void stopMockServer() {
        wireMockServer.stop();
    }

     **/

    @Test
    void testFileFromClasspath() {

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        OpenAPI openAPI = new OpenAPIV3Parser().readLocation("example-get-noparams.yaml", null, parseOptions).getOpenAPI();

        assertNotNull(openAPI);
    }

    @Test
    void testGetNoParams() {

      Map<ToolSpecification, ToolExecutor> specifications = OpenAPIToolsImporter.builder()
              .withToolName("testTool")
              .withSpecPath("example-get-noparams.yaml")
              .build()
              .getSpecificationsMaps();

        assertNotNull(specifications);
        assertEquals(1,specifications.size());

        ToolSpecification toolSpecification = specifications.keySet().iterator().next();

        assertEquals("testTool-getUsers", toolSpecification.name());
        assertEquals("Returns a list of users.",toolSpecification.description());

    }
    @Test
    void testGetUserById() {

        Map<ToolSpecification, ToolExecutor> specifications = OpenAPIToolsImporter.builder()
                .withToolName("testTool")
                .withSpecPath("example-get-pathparams.yaml")
                .build()
                .getSpecificationsMaps();

        assertNotNull(specifications);
        assertEquals(2, specifications.size());

        ToolSpecification toolSpecification = specifications.keySet().stream()
                .filter(spec -> spec.name().equals("testTool-getUserById"))
                .findFirst()
                .orElse(null);

        assertNotNull(toolSpecification);
        assertEquals("testTool-getUserById", toolSpecification.name());
        assertEquals("Returns a single user.", toolSpecification.description());

        JsonObjectSchema parameters = toolSpecification.parameters();
        Map<String, JsonSchemaElement> properties = parameters.properties();
        JsonSchemaElement userIdProperty = properties.get("userId");

        assertInstanceOf(JsonStringSchema.class,userIdProperty);
        JsonStringSchema userIdSchema = (JsonStringSchema) userIdProperty;
        assertEquals("User identification number",userIdSchema.description());


    }

    @Test
    void testGetAndPost() {

        Map<ToolSpecification, ToolExecutor> specifications = OpenAPIToolsImporter.builder()
                .withToolName("testTool")
                .withSpecPath("example-get-post.yaml")
                .build()
                .getSpecificationsMaps();

        assertNotNull(specifications);
        assertEquals(2, specifications.size());

        ToolSpecification toolSpecification = specifications.keySet().stream()
                .filter(spec -> spec.name().equals("testTool-getTransactionsByRecipientName"))
                .findFirst()
                .orElse(null);

        assertNotNull(toolSpecification);
        assertEquals("testTool-getTransactionsByRecipientName", toolSpecification.name());
        assertEquals("Gets the transactions lists. They can be filtered based on recipient name", toolSpecification.description());

        JsonObjectSchema parameters = toolSpecification.parameters();
        Map<String, JsonSchemaElement> properties = parameters.properties();
        JsonSchemaElement accountIdProperty = properties.get("accountid");

        assertInstanceOf(JsonIntegerSchema.class,accountIdProperty);
        JsonIntegerSchema accountIdSchema = (JsonIntegerSchema) accountIdProperty;
        assertEquals("id of specific account.",accountIdSchema.description());


    }

    @Test
    void testPostWithRequestBody() {

        Map<ToolSpecification, ToolExecutor> specifications = OpenAPIToolsImporter.builder()
                .withToolName("testTool")
                .withSpecPath("example-post-requestbody.yaml")
                .build()
                .getSpecificationsMaps();

        assertNotNull(specifications);
        assertEquals(1, specifications.size());

        ToolSpecification toolSpecification = specifications.keySet().stream()
                .filter(spec -> spec.name().equals("testTool-submitPayment"))
                .findFirst()
                .orElse(null);

        assertNotNull(toolSpecification);
        assertEquals("testTool-submitPayment", toolSpecification.name());
        assertEquals("Submit a payment request", toolSpecification.description());

        JsonObjectSchema parameters = toolSpecification.parameters();
        Map<String, JsonSchemaElement> properties = parameters.properties();

        JsonSchemaElement requestBody = properties.get("requestBody");
        assertInstanceOf(JsonObjectSchema.class,requestBody);
        JsonObjectSchema requestBodySchema = (JsonObjectSchema) requestBody;
        Map<String, JsonSchemaElement> requestBodyProperties = requestBodySchema.properties();

        JsonSchemaElement recipientName = requestBodyProperties.get("recipientName");
        assertInstanceOf(JsonStringSchema.class,recipientName);
        JsonStringSchema recipientNameSchema = (JsonStringSchema) recipientName;
        assertEquals("Name of the recipient",recipientNameSchema.description());

        JsonSchemaElement accountId = requestBodyProperties.get("accountId");
        assertInstanceOf(JsonIntegerSchema.class,accountId);
        JsonIntegerSchema userIdSchema = (JsonIntegerSchema) accountId;
        assertEquals("ID of the account",userIdSchema.description());

        JsonSchemaElement amount = requestBodyProperties.get("amount");
        assertInstanceOf(JsonNumberSchema.class,amount);
        JsonNumberSchema amountSchema = (JsonNumberSchema) amount;
        assertEquals("Amount of the payment",amountSchema.description());

    }

    /** it won't work till we are able to configure an http client for langchain4j which accepts wiremock self signed certificate **/
  //  @Test
    void testOpenAIFlow(){

        String expectedJson = """
                {
                  "model" : "gpt-4o-mini",
                  "messages" : [ {
                    "role" : "user",
                    "content" : "need user info for id 1020"
                  } ],
                  "stream" : false,
                  "tools" : [ {
                    "type" : "function",
                    "function" : {
                      "name" : "testTool-getUsers",
                      "description" : "Returns a list of users.",
                      "parameters" : {
                        "type" : "object",
                        "properties" : { },
                        "required" : [ ]
                      }
                    }
                  }, {
                    "type" : "function",
                    "function" : {
                      "name" : "testTool-getUserById",
                      "description" : "Returns a single user.",
                      "parameters" : {
                        "type" : "object",
                        "properties" : {
                          "userId" : {
                            "type" : "string",
                            "description" : "User identification number"
                          }
                        },
                        "required" : [ "userId" ]
                      }
                    }
                  } ]
                }
                """ ;
      /**
        wireMockServer.stubFor(get(urlEqualTo("chat/completions"))
                        .withRequestBody( equalToJson(expectedJson))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));
        **/

        Map<ToolSpecification, ToolExecutor> specifications = OpenAPIToolsImporter.builder()
                .withToolName("testTool")
                .withSpecPath("example-post-requestbody.yaml")
                .build()
                .getSpecificationsMaps();

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey("fakekey")
                .modelName("gpt-4o-mini")
                //.baseUrl("https://localhost:8443")
                .logRequests(true)
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from("need user info for id 1020"))
                .toolSpecifications(new ArrayList<>(specifications.keySet()))
                .build();
        model.chat(request);
    }


}
