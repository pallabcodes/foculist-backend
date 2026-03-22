package com.yourorg.platform.foculist.project.infrastructure.messaging;

import com.yourorg.platform.foculist.project.application.CreateProjectCommand;
import com.yourorg.platform.foculist.project.application.ProjectApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceCreatedEventConsumer {

    private final ProjectApplicationService projectApplicationService;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleWorkspaceCreated(Map<String, Object> payload) {
        log.info("📥 Received WORKSPACE_CREATED Event: {}", payload);

        try {
            String tenantId = (String) payload.get("tenantId");
            String projectName = (String) payload.get("projectName");
            String projectKey = (String) payload.get("projectKey");
            String experience = (String) payload.get("experience");
            String ownerId = (String) payload.get("ownerId");

            if (tenantId == null || projectName == null) {
                log.error("❌ Invalid payload: missing tenantId or projectName");
                return;
            }

            // Map to CreateProjectCommand
            CreateProjectCommand command = new CreateProjectCommand(
                    projectName,
                    "Auto-generated workspace for " + experience,
                    "RUNNING", // Default Status
                    "MEDIUM",  // Default Priority
                    null,      // No Due Date
                    ownerId != null ? UUID.fromString(ownerId) : null,
                    projectKey,
                    null       // Use default permission scheme
            );

            log.info("🗂️ Creating Default Project for Tenant: '{}', Project: '{}'", tenantId, projectName);
            projectApplicationService.createProject(tenantId, command);
            log.info("✅ Default Project created successfully for Tenant: '{}'", tenantId);

        } catch (Exception e) {
            log.error("❌ Failed to process WorkspaceCreatedEvent", e);
            // In production, you might want to throw to trigger RabbitMQ retry/DLQ, 
            // but for now, we catch to prevent Infinite Loops if payload is corrupt.
        }
    }
}
