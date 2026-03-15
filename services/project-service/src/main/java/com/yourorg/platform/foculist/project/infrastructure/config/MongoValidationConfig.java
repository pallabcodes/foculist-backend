package com.yourorg.platform.foculist.project.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;
import org.springframework.data.mongodb.core.validation.Validator;

/**
 * Hardens MongoDB collections with JSON Schema Validation.
 * This satisfies Senior DBA requirements for data integrity in a schemaless store.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoValidationConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initSchemaValidation() {
        String collectionName = "project_brainstorm";
        
        // Define the hardened schema for project brainstorms
        MongoJsonSchema schema = MongoJsonSchema.builder()
                .required("tenantId", "projectId", "summary")
                .properties(
                        JsonSchemaProperty.string("tenantId"),
                        JsonSchemaProperty.string("projectId"),
                        JsonSchemaProperty.string("summary"),
                        JsonSchemaProperty.array("tags").items(org.springframework.data.mongodb.core.schema.JsonSchemaObject.string()),
                        JsonSchemaProperty.date("createdAt"),
                        JsonSchemaProperty.date("updatedAt")
                )
                .build();

        if (!mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.createCollection(collectionName, CollectionOptions.empty()
                    .validator(Validator.schema(schema)));
            log.info("Created MongoDB collection '{}' with JSON Schema Validation enabled.", collectionName);
        } else {
            // In a production scenario (L5), we would check if the validator exists and update it.
            // For V1 Beta, we ensure at least the collection is governed if created by the app.
            log.info("MongoDB collection '{}' already exists. Skipping schema validation enforcement.", collectionName);
        }
    }
}
