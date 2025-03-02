package com.example.validate;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class MyConfig {

    @Bean
    public GraphQL graphQL() throws IOException {
        File schemaFile = new File("C:\\Users\\shagu\\Downloads\\validate\\src\\main\\resources\\graphql\\schema.graphqls");
        String schemaDefinition = new String(java.nio.file.Files.readAllBytes(schemaFile.toPath()));

        // Parse the schema definition
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry schemaRegistry = schemaParser.parse(schemaDefinition);

        // Create the GraphQL schema
        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(schemaRegistry, RuntimeWiring.newRuntimeWiring().build());

        // Create the GraphQL instance
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();
        return graphQL;
    }
}
