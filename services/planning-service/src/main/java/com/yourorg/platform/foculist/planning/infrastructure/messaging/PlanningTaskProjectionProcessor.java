package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.domain.event.TaskCreatedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskDeletedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskEventTypes;
import com.yourorg.platform.foculist.planning.domain.event.TaskStatusChangedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskUpdatedEvent;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.model.ProjectionCheckpoint;
import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.ProjectionCheckpointRepositoryPort;
import com.yourorg.platform.foculist.planning.infrastructure.persistence.mongodb.TaskProjectionDocument;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanningTaskProjectionProcessor {
    private static final Logger log = LoggerFactory.getLogger(PlanningTaskProjectionProcessor.class);
    private static final String PROJECTION_NAME = "planning-task-list";

    private final OutboxEventRepositoryPort outboxEventRepository;
    private final ProjectionCheckpointRepositoryPort checkpointRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${app.event-sourcing.projection-batch-size:100}")
    private int batchSize;

    public PlanningTaskProjectionProcessor(
            OutboxEventRepositoryPort outboxEventRepository,
            ProjectionCheckpointRepositoryPort checkpointRepository,
            MongoTemplate mongoTemplate,
            ObjectMapper objectMapper,
            org.springframework.beans.factory.ObjectProvider<Clock> clockProvider
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.checkpointRepository = checkpointRepository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.clock = clockProvider.getIfAvailable(Clock::systemUTC);
    }

    @Scheduled(fixedDelayString = "${app.event-sourcing.projection-poll-interval-ms:2000}")
    @Transactional
    public void project() {
        ProjectionCheckpoint checkpoint = checkpointRepository.findOrCreate(PROJECTION_NAME);
        List<OutboxEvent> events = outboxEventRepository.findBatchAfter(
                checkpoint.lastOccurredAt(),
                checkpoint.lastEventId(),
                batchSize
        );
        if (events.isEmpty()) {
            return;
        }

        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TaskProjectionDocument.class);
        boolean hasOps = false;

        for (OutboxEvent event : events) {
            if (addOp(ops, event)) {
                hasOps = true;
            }
            checkpoint = checkpoint.advance(event.occurredAt(), event.id(), Instant.now(clock));
        }

        if (hasOps) {
            ops.execute();
        }

        checkpointRepository.save(checkpoint);
        log.info("Projected {} task events into MongoDB read model via BulkOps", events.size());
    }

    private boolean addOp(BulkOperations ops, OutboxEvent event) {
        try {
            switch (event.eventType()) {
                case TaskEventTypes.TASK_CREATED -> {
                    TaskCreatedEvent payload = objectMapper.readValue(event.payload(), TaskCreatedEvent.class);
                    Query query = query(payload.taskId(), payload.tenantId());
                    Update update = new Update()
                            .set("taskId", payload.taskId())
                            .set("tenantId", payload.tenantId())
                            .set("sprintId", payload.sprintId())
                            .set("title", payload.title())
                            .set("description", payload.description())
                            .set("status", TaskStatus.from(payload.status()))
                            .set("priority", TaskPriority.from(payload.priority()))
                            .set("createdAt", payload.occurredAt())
                            .set("updatedAt", payload.occurredAt())
                            .set("version", payload.version());
                    ops.upsert(query, update);
                    return true;
                }
                case TaskEventTypes.TASK_UPDATED -> {
                    TaskUpdatedEvent payload = objectMapper.readValue(event.payload(), TaskUpdatedEvent.class);
                    Query query = query(payload.taskId(), payload.tenantId());
                    Update update = new Update()
                            .set("sprintId", payload.sprintId())
                            .set("title", payload.title())
                            .set("description", payload.description())
                            .set("priority", TaskPriority.from(payload.priority()))
                            .set("updatedAt", payload.occurredAt())
                            .set("version", payload.version());
                    ops.updateOne(query, update);
                    return true;
                }
                case TaskEventTypes.TASK_STATUS_CHANGED -> {
                    TaskStatusChangedEvent payload = objectMapper.readValue(event.payload(), TaskStatusChangedEvent.class);
                    Query query = query(payload.taskId(), payload.tenantId());
                    Update update = new Update()
                            .set("status", TaskStatus.from(payload.newStatus()))
                            .set("updatedAt", payload.occurredAt())
                            .set("version", payload.version());
                    ops.updateOne(query, update);
                    return true;
                }
                case TaskEventTypes.TASK_DELETED -> {
                    TaskDeletedEvent payload = objectMapper.readValue(event.payload(), TaskDeletedEvent.class);
                    ops.remove(query(payload.taskId(), payload.tenantId()));
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse event payload for event id: {}", event.id(), ex);
            return false;
        }
    }

    private Query query(UUID taskId, String tenantId) {
        return Query.query(Criteria.where("taskId").is(taskId).and("tenantId").is(tenantId));
    }
}
