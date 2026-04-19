package com.yourorg.platform.foculist.project.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Slf4j
public class WorkspaceEventListener {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "foculist.project.workspace-events", durable = "true"),
            exchange = @Exchange(value = "foculist.workspace.events", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "workspace.created"
    ))
    public void handleWorkspaceCreated(Map<String, Object> messagePayload) {
        try {
            String tenantId = (String) messagePayload.get("tenantId");
            String workspaceName = (String) messagePayload.get("name");
            log.info("Received workspace.created for tenant: {} (workspace: {}). Auto-provisioning default Project structure...", tenantId, workspaceName);
        } catch (Exception e) {
            log.error("Failed to process workspace event in project service", e);
        }
    }
}
