package com.example.validate;

import java.util.Map;

public class GraphQLValidationRequest {
    private String query;
    private Map<String, Object> variables; // optional

    // Getters and setters
    public String getQuery() {
        return query;
    }
    public void setQuery(String query) {
        this.query = query;
    }
    public Map<String, Object> getVariables() {
        return variables;
    }
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
