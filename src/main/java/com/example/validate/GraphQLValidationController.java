package com.example.validate;

import graphql.GraphQL;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class GraphQLValidationController {

    @PostMapping("/validate")
    public ResponseEntity<?> validateQuery(@RequestBody GraphQLValidationRequest request) {
        try {
            // Build the dynamic GraphQL schema from SDL
            GraphQLSchema schema = buildSchema();

            // Parse the incoming GraphQL query
            Parser parser = new Parser();
            Document document = parser.parseDocument(request.getQuery());

            // Validate the parsed query against the schema structure
            Validator validator = new Validator();
            List<ValidationError> errors = validator.validateDocument(schema, document, Locale.ENGLISH);

            if (!errors.isEmpty()) {
                List<String> errorMessages = errors.stream()
                        .map(ValidationError::getMessage)
                        .collect(Collectors.toList());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessages);
            }

            // Validate variables using the GraphQL type system.
            // For our schema, we expect a variable "input" of type [UserInput!]!
            GraphQLObjectType queryType = schema.getQueryType();
            GraphQLFieldDefinition fieldDefinition = queryType.getFieldDefinition("validateUsersList");
            GraphQLArgument inputArg = fieldDefinition.getArgument("input");
            GraphQLInputType expectedInputType = inputArg.getType();

            Object inputVariable = request.getVariables().get("input");
            List<String> variableErrors = validateValue(inputVariable, expectedInputType, "input");

            if (!variableErrors.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(variableErrors);
            }

            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }

    /**
     * Recursively validates a given input value against a GraphQLInputType, accumulating error messages.
     *
     * @param inputValue the value to validate (from query variables)
     * @param type       the expected GraphQL input type
     * @param path       the current path (e.g. "input", "input[0]", "input[0].username")
     * @return a list of error messages detailing the validation failures.
     */
    private List<String> validateValue(Object inputValue, GraphQLInputType type, String path) {
        List<String> errors = new ArrayList<>();

        // Handle Non-Null type: unwrap and ensure value is not null.
        if (GraphQLTypeUtil.isNonNull(type)) {
            GraphQLInputType unwrapped = (GraphQLInputType) GraphQLTypeUtil.unwrapNonNull(type);
            if (inputValue == null) {
                errors.add(String.format("Error at %s: Null value provided for non-null type %s", path, type.getClass()));
            } else {
                errors.addAll(validateValue(inputValue, unwrapped, path));
            }
            return errors;
        }

        // Handle Scalar types using the coercing logic.
        if (type instanceof GraphQLScalarType) {
            GraphQLScalarType scalarType = (GraphQLScalarType) type;
            try {
                scalarType.getCoercing().parseValue(inputValue);
            } catch (Exception e) {
                String providedType = inputValue == null ? "null" : inputValue.getClass().getSimpleName();
                errors.add(String.format("Error at %s: Value '%s' is not valid for scalar type %s. Provided datatype: %s, expected: %s",
                        path, inputValue, scalarType.getClass(), providedType, scalarType.getName()));
            }
            return errors;
        }

        // Handle Input Object types by validating each field.
        if (type instanceof GraphQLInputObjectType) {
            if (!(inputValue instanceof Map)) {
                errors.add(String.format("Error at %s: Expected an object (Map) for input object type %s", path, type.getClass()));
                return errors;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = (Map<String, Object>) inputValue;
            GraphQLInputObjectType inputObjectType = (GraphQLInputObjectType) type;
            for (GraphQLInputObjectField field : inputObjectType.getFields()) {
                Object fieldValue = inputMap.get(field.getName());
                if (GraphQLTypeUtil.isNonNull(field.getType()) && fieldValue == null) {
                    errors.add(String.format("Error at %s.%s: Missing required field", path, field.getName()));
                }
                if (fieldValue != null) {
                    errors.addAll(validateValue(fieldValue, field.getType(), path + "." + field.getName()));
                }
            }
            return errors;
        }

        // Handle List types by validating each item in the list.
        if (type instanceof GraphQLList) {
            if (!(inputValue instanceof List)) {
                errors.add(String.format("Error at %s: Expected a List for type %s", path, type.getClass()));
                return errors;
            }
            GraphQLList listType = (GraphQLList) type;
            GraphQLInputType wrappedType = (GraphQLInputType) listType.getWrappedType();
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) inputValue;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                errors.addAll(validateValue(item, wrappedType, path + "[" + i + "]"));
            }
            return errors;
        }

        // Unsupported type (should not normally happen)
        errors.add(String.format("Error at %s: Unsupported GraphQL input type %s", path, type.getClass()));
        return errors;
    }

    /**
     * Builds the executable GraphQL schema from SDL.
     */
    private GraphQLSchema buildSchema() {
        String sdl = loadDynamicSchema();
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        // No custom data fetchers are required for validation
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring().build();
        return new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
    }

    /**
     * Provides a sample dynamic GraphQL schema in SDL.
     */
    private String loadDynamicSchema() {
        return "type ValidationResult {\n" +
                "  index: Int\n" +
                "  inputType: String\n" +
                "  valid: Boolean\n" +
                "  message: String\n" +
                "  invalidFields: [String]\n" +
                "}\n" +
                "\n" +
                "input UserInput {\n" +
                "  username: String\n" +
                "  email: String\n" +
                "  age: Int\n" +
                "}\n" +
                "\n" +
                "type Query {\n" +
                "  validateUsersList(input: [UserInput!]!): [ValidationResult]\n" +
                "}";
    }
}
