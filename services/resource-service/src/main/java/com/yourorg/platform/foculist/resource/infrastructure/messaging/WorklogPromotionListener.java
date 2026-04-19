package com.yourorg.platform.foculist.resource.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateWorklogCommand;
import com.yourorg.platform.foculist.resource.clean.application.service.ResourceApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorklogPromotionListener {

    private final ResourceApplicationService resourceService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitResilienceConfig.PROMOTED_WORKLOGS_QUEUE)
    public void handleWorklogPromoted(String messagePayload) {
        try {
            log.info("Received promoted worklog from Meeting Service");
            JsonNode node = objectMapper.readTree(messagePayload);
            String tenantId = node.get("tenantId").asText();
            String project = node.has("project") ? node.get("project").asText() : "default";
            String task = node.get("task").asText();
            int durationMinutes = node.has("durationMinutes") ? node.get("durationMinutes").asInt() : 0;

            resourceService.createWorklog(tenantId, new CreateWorklogCommand(project, task, durationMinutes));
            log.info("Successfully created promoted worklog for task: {}", task);
        } catch (Exception e) {
            log.error("Failed to process promoted worklog message. Routing to DLQ.", e);
            throw new RuntimeException("Re-throwing for DLQ routing", e);
        }
    }
}
