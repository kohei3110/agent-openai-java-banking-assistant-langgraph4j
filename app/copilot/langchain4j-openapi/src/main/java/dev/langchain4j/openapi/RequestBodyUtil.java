package dev.langchain4j.openapi;

import dev.langchain4j.model.chat.request.json.*;
import io.swagger.v3.oas.models.media.*;

import java.util.HashMap;

import java.util.Map;

public class RequestBodyUtil {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RequestBodyUtil.class);


    public static JsonSchemaElement buildRequestBodySchema(Schema<?> schema) {

        Map<String, JsonSchemaElement> properties = new HashMap<>();
                Map<String,Schema> schemaProperties = schema.getProperties();
        schemaProperties.forEach((propertyName, propertySchema) -> {
            properties.put(propertyName,getJsonSchemaElement(propertySchema,propertySchema.getDescription()));
        });

        return JsonObjectSchema.builder().addProperties(properties).required(schema.getRequired()).build();

    }

    private static JsonSchemaElement getJsonSchemaElement(Schema schema, String description) {

        String t = schema.getType();
        JsonSchemaElement jsonSchemaElement = null;
        if (t != null) {
            switch (t) {
                case "integer":
                    jsonSchemaElement = JsonIntegerSchema.builder().description(description).build();
                    break;
                case "string":
                    jsonSchemaElement = JsonStringSchema.builder().description(description).build();
                    break;
                case "number":
                    jsonSchemaElement = JsonNumberSchema.builder().description(description).build();
                    break;
                case "boolean":
                    jsonSchemaElement = JsonBooleanSchema.builder().description(description).build();
                    break;
                case "array":
                    jsonSchemaElement = JsonArraySchema.builder().description(description).build();
                    break;
                // Not sure if we can support this
                case "null":
                    break;
                case "object":
                    jsonSchemaElement = JsonObjectSchema.builder().description(description).build();
                default:
                    break;
            }
        }
        return jsonSchemaElement;
    }


    public static String renderJsonExample(Schema<?> schema) {
        StringBuilder stringBuilder = new StringBuilder();
        renderJsonExample(stringBuilder, "root", schema);
        return "{" + stringBuilder + "}";
    }

    public static void renderJsonExample(
            StringBuilder stringBuilder,
            String valueKey,
            Schema<?> schema) {

        if (schema.getExample() != null) {
            if (valueKey != null) {
                stringBuilder.append("\"").append(valueKey).append("\": ");
            }

            if (schema instanceof StringSchema) {
                stringBuilder.append("\"").append(schema.getExample().toString()).append("\",");
            } else {
                stringBuilder.append(schema.getExample().toString()).append(",");
            }
            return;
        }

        if (schema instanceof IntegerSchema ||
                schema instanceof StringSchema ||
                schema instanceof NumberSchema ||
                schema instanceof BooleanSchema) {

            String value;

            if (schema instanceof IntegerSchema) {
                value = "1";
            } else if (schema instanceof StringSchema) {
                if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                    value = "\"" + schema.getEnum().get(0).toString() + "\"";
                } else {
                    value = "\"string\"";
                }
            } else if (schema instanceof NumberSchema) {
                value = "1.0";
            } else {
                value = "true";
            }

            if (valueKey != null) {
                stringBuilder.append("\"").append(valueKey).append("\": ");
            }
            stringBuilder.append(value).append(",");

        } else if (schema instanceof ObjectSchema objectSchema) {

            if (valueKey != null) {
                stringBuilder.append("\"").append(valueKey).append("\": ");
            }

            stringBuilder.append("{");

            objectSchema.getProperties().forEach((key, value) -> {
                renderJsonExample(stringBuilder, key, value);
            });
            stringBuilder.append("},");
        } else if (schema instanceof ArraySchema arraySchema) {

            if (valueKey != null) {
                stringBuilder.append("\"").append(valueKey).append("\": ");
            }

            stringBuilder.append("[");
            renderJsonExample(stringBuilder, null,
                    arraySchema.getItems());
            stringBuilder.append("],");

        } else if (schema instanceof MapSchema) {
            throw new IllegalArgumentException("Not yet supported");
        } else {
            LOGGER.warn("Unsupported schema type {}", schema.getClass().getName());
        }
    }

    public static String renderXmlExample(Schema<?> schema) {
        StringBuilder stringBuilder = new StringBuilder();
        renderXmlExample(stringBuilder, null, schema);
        return stringBuilder.toString();
    }

    public static void renderXmlExample(StringBuilder stringBuilder, String valueKey,
                                         Schema<?> schema) {

        if (schema.getExample() != null) {
            stringBuilder.append("<").append(valueKey).append(">");
            stringBuilder.append(schema.getExample().toString());
            stringBuilder.append("</").append(valueKey).append(">");
            return;
        }

        if (schema instanceof IntegerSchema ||
                schema instanceof StringSchema ||
                schema instanceof NumberSchema ||
                schema instanceof BooleanSchema) {

            String value;

            if (schema instanceof IntegerSchema) {
                value = "1";
            } else if (schema instanceof StringSchema) {
                if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                    value = schema.getEnum().get(0).toString();
                } else {
                    value = "string";
                }
            } else if (schema instanceof NumberSchema) {
                value = "1.0";
            } else {
                value = "true";
            }

            stringBuilder.append("<").append(valueKey).append(">");
            stringBuilder.append(value);
            stringBuilder.append("</").append(valueKey).append(">");
        } else if (schema instanceof ObjectSchema objectSchema) {

            String nameTag = "<" + schema.getXml().getName() + ">";
            String closingNameTag = "</" + schema.getXml().getName() + ">";

            stringBuilder.append(nameTag);
            objectSchema.getProperties().forEach((key, value) -> {
                renderXmlExample(stringBuilder, key, value);
            });
            stringBuilder.append(closingNameTag);
        } else if (schema instanceof ArraySchema arraySchema) {
            String nameTag = "<" + valueKey + ">";
            String closingNameTag = "</" + valueKey + ">";
            stringBuilder.append(nameTag);
            renderXmlExample(stringBuilder, arraySchema.getItems().getXml().getName(),
                    arraySchema.getItems());
            stringBuilder.append(closingNameTag);
        } else if (schema instanceof MapSchema) {
            throw new IllegalArgumentException("Not yet supported");
        } else {
            LOGGER.warn("Unsupported schema type {}", schema.getClass().getName());
        }
    }
}
