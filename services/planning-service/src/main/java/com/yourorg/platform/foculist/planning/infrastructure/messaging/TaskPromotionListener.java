package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.application.CreateTaskCommand;
import com.yourorg.platform.foculist.planning.application.PlanningApplicationService;
import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
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
public class TaskPromotionListener {

    private final PlanningApplicationService planningService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitResilienceConfig.PROMOTED_TASKS_QUEUE)
    public void handleTaskPromoted(String messagePayload) {
        try {
            log.info("Received promoted task from Meeting Service");
            JsonNode node = objectMapper.readTree(messagePayload);
            String tenantId = node.get("tenantId").asText();
            String title = node.get("title").asText();
            String description = node.has("description") ? node.get("description").asText() : "";
            String priority = node.has("priority") ? node.get("priority").asText() : "MEDIUM";
            
            planningService.createTask(tenantId, new CreateTaskCommand(title, description, "TODO", priority, null));
            log.info("Successfully created promoted task: {}", title);
        } catch (Exception e) {
            log.error("Failed to process promoted task message. Routing to DLQ.", e);
            throw new RuntimeException("Re-throwing for DLQ routing", e);
        }
    }
}
