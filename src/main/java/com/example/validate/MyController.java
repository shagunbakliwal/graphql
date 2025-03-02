package com.example.validate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/graphql")
public class MyController {
    @Autowired
    DynamicSchemaValidationResolver dynamicSchemaValidationResolver;

    @PostMapping
    public List<ValidationResult> validateQuery(@RequestBody Map<String, Object> requestBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        // Extract the variables part of the GraphQL request
        JsonNode variables = new ObjectMapper().valueToTree(requestBody.get("variables"));

        /*if (variables == null || !variables.has("input")) {
            return Map.of("status", "error", "message", "Missing 'input' field in GraphQL query.");
        }*/

        // Convert input to List<Map<String, Object>>
        List<Map<String, Object>> inputList = objectMapper.convertValue(variables.get("request"), List.class);

        List<ValidationResult> validationResults = dynamicSchemaValidationResolver.validateDynamic(inputList);
        return validationResults;
    }

    //@PostMapping("/")
    public List<ValidationResult> dd(@RequestBody GraphQLRequest request) throws IOException {
        String query = request.getQuery();

        // Extract JSON array part from query string using regex
        Pattern pattern = Pattern.compile("input:\\s*(\\[.*?\\])", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            String inputJson = matcher.group(1);
            ObjectMapper objectMapper = new ObjectMapper();
            // Convert extracted JSON string to List<Map<String, Object>>
            List<Map<String, Object>> inputList = objectMapper.readValue(inputJson, new TypeReference<>() {
            });
            /*List<ValidationResult> validationResults = dynamicSchemaValidationResolver.validateUsersList(inputList);
            return validationResults;*/
            return new ArrayList<>();
        } else {
            throw new IllegalArgumentException("Invalid GraphQL query format");
        }

    }
}
