package com.example.validate;

import graphql.GraphQL;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicSchemaValidationResolver implements GraphQLQueryResolver {

    @Autowired
    private GraphQL graphQL; // Inject the GraphQL instance

    @QueryMapping
    public List<ValidationResult> validateDynamic(List<Map<String, Object>> input) {
        List<ValidationResult> results = new ArrayList<>();
        Map<String, GraphQLType> expectedFields = getExpectedFields("UserInput");

        if (expectedFields == null) {
            return List.of(new ValidationResult(-1, "UserInput", false, "Invalid input type", List.of("Unknown type")));
        }

        for (int i = 0; i < input.size(); i++) {
            Map<String, Object> userMap = input.get(i);
            List<String> invalidFields = new ArrayList<>();
            Map<String, Object> fixedUser = autofixDynamicInput(userMap, expectedFields, invalidFields);

            boolean isValid = invalidFields.isEmpty();
            String message = isValid ? "Valid input" : "Invalid fields detected: " + invalidFields;

            results.add(new ValidationResult(i, "UserInput", isValid, message, invalidFields));
        }

        return results;
    }

    private Map<String, GraphQLType> getExpectedFields(String inputType) {
        GraphQLSchema schema = graphQL.getGraphQLSchema();
        GraphQLType type = schema.getType(inputType);

        if (!(type instanceof GraphQLInputObjectType)) {
            return null;
        }

        GraphQLInputObjectType inputObjectType = (GraphQLInputObjectType) type;
        return inputObjectType.getFieldDefinitions().stream()
                .collect(Collectors.toMap(GraphQLInputObjectField::getName, GraphQLInputObjectField::getType));
    }

    private Map<String, Object> autofixDynamicInput(Map<String, Object> userMap, Map<String, GraphQLType> expectedFields, List<String> invalidFields) {
        Map<String, Object> fixedInput = new HashMap<>();

        for (Map.Entry<String, GraphQLType> entry : expectedFields.entrySet()) {
            String fieldName = entry.getKey();
            GraphQLType expectedType = entry.getValue();

            if (!userMap.containsKey(fieldName)) {
                invalidFields.add(fieldName);
                fixedInput.put(fieldName, getDefaultForType(expectedType));
                continue;
            }

            Object value = userMap.get(fieldName);
            if (!isValidType(value, expectedType)) {
                invalidFields.add(fieldName);
                fixedInput.put(fieldName, getDefaultForType(expectedType));
            } else {
                fixedInput.put(fieldName, value);
            }
        }

        return fixedInput;
    }

    private boolean isValidType(Object value, GraphQLType expectedType) {
        if (value == null) return false;

        if (expectedType instanceof GraphQLNonNull) {
            expectedType = ((GraphQLNonNull) expectedType).getWrappedType();
        }

        if (expectedType instanceof GraphQLScalarType) {
            GraphQLScalarType scalarType = (GraphQLScalarType) expectedType;
            return switch (scalarType.getName()) {
                case "String" -> value instanceof String;
                case "Int" -> value instanceof Integer || (value instanceof String && isInteger((String) value));
                case "Boolean" -> value instanceof Boolean;
                case "Float" -> value instanceof Double || value instanceof Float;
                default -> false;
            };
        }

        return false;
    }

    private Object getDefaultForType(GraphQLType expectedType) {
        if (expectedType instanceof GraphQLNonNull) {
            expectedType = ((GraphQLNonNull) expectedType).getWrappedType();
        }

        if (expectedType instanceof GraphQLScalarType) {
            GraphQLScalarType scalarType = (GraphQLScalarType) expectedType;
            return switch (scalarType.getName()) {
                case "String" -> "default_value";
                case "Int" -> 0;
                case "Boolean" -> false;
                case "Float" -> 0.0;
                default -> null;
            };
        }

        return null;
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
