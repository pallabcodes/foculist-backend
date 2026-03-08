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
import com.yourorg.platform.foculist.planning.domain.model.TaskProjection;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.ProjectionCheckpointRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskProjectionRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanningTaskProjectionProcessor {
    private static final Logger log = LoggerFactory.getLogger(PlanningTaskProjectionProcessor.class);
    private static final String PROJECTION_NAME = "planning-task-list";

    private final OutboxEventRepositoryPort outboxEventRepository;
    private final TaskProjectionRepositoryPort taskProjectionRepository;
    private final ProjectionCheckpointRepositoryPort checkpointRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${app.event-sourcing.projection-batch-size:100}")
    private int batchSize;

    public PlanningTaskProjectionProcessor(
            OutboxEventRepositoryPort outboxEventRepository,
            TaskProjectionRepositoryPort taskProjectionRepository,
            ProjectionCheckpointRepositoryPort checkpointRepository,
            ObjectMapper objectMapper
    ) {
        this(outboxEventRepository, taskProjectionRepository, checkpointRepository, objectMapper, Clock.systemUTC());
    }

    PlanningTaskProjectionProcessor(
            OutboxEventRepositoryPort outboxEventRepository,
            TaskProjectionRepositoryPort taskProjectionRepository,
            ProjectionCheckpointRepositoryPort checkpointRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.taskProjectionRepository = taskProjectionRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
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

        for (OutboxEvent event : events) {
            apply(event);
            checkpoint = checkpoint.advance(event.occurredAt(), event.id(), Instant.now(clock));
            checkpointRepository.save(checkpoint);
        }
        log.info("Projected {} task events into read model", events.size());
    }

    private void apply(OutboxEvent event) {
        try {
            switch (event.eventType()) {
                case TaskEventTypes.TASK_CREATED -> {
                    TaskCreatedEvent payload = objectMapper.readValue(event.payload(), TaskCreatedEvent.class);
                    taskProjectionRepository.save(new TaskProjection(
                            payload.taskId(),
                            payload.tenantId(),
                            payload.sprintId(),
                            payload.title(),
                            payload.description(),
                            TaskStatus.from(payload.status()),
                            TaskPriority.from(payload.priority()),
                            payload.occurredAt(),
                            payload.occurredAt(),
                            payload.version()
                    ));
                }
                case TaskEventTypes.TASK_UPDATED -> {
                    TaskUpdatedEvent payload = objectMapper.readValue(event.payload(), TaskUpdatedEvent.class);
                    TaskProjection existing = taskProjectionRepository.findByIdAndTenantId(payload.taskId(), payload.tenantId())
                            .orElseThrow();
                    taskProjectionRepository.save(new TaskProjection(
                            existing.id(),
                            existing.tenantId(),
                            payload.sprintId(),
                            payload.title(),
                            payload.description(),
                            existing.status(),
                            TaskPriority.from(payload.priority()),
                            existing.createdAt(),
                            payload.occurredAt(),
                            payload.version()
                    ));
                }
                case TaskEventTypes.TASK_STATUS_CHANGED -> {
                    TaskStatusChangedEvent payload = objectMapper.readValue(event.payload(), TaskStatusChangedEvent.class);
                    TaskProjection existing = taskProjectionRepository.findByIdAndTenantId(payload.taskId(), payload.tenantId())
                            .orElseThrow();
                    taskProjectionRepository.save(new TaskProjection(
                            existing.id(),
                            existing.tenantId(),
                            existing.sprintId(),
                            existing.title(),
                            existing.description(),
                            TaskStatus.from(payload.newStatus()),
                            existing.priority(),
                            existing.createdAt(),
                            payload.occurredAt(),
                            payload.version()
                    ));
                }
                case TaskEventTypes.TASK_DELETED -> {
                    TaskDeletedEvent payload = objectMapper.readValue(event.payload(), TaskDeletedEvent.class);
                    taskProjectionRepository.deleteByIdAndTenantId(payload.taskId(), payload.tenantId());
                }
                default -> {
                    return;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to project outbox event " + event.id(), ex);
        }
    }
}
