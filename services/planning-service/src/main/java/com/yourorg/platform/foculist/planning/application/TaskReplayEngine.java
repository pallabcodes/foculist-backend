package com.yourorg.platform.foculist.planning.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.planning.domain.event.TaskCreatedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskDeletedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskEventTypes;
import com.yourorg.platform.foculist.planning.domain.event.TaskStatusChangedEvent;
import com.yourorg.platform.foculist.planning.domain.event.TaskUpdatedEvent;
import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import com.yourorg.platform.foculist.planning.domain.model.PlanningDomainException;
import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshot;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotRepositoryPort;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TaskReplayEngine {
    private static final String TASK_AGGREGATE_TYPE = "Task";

    private final EventStoreRepositoryPort eventStoreRepository;
    private final TaskSnapshotRepositoryPort taskSnapshotRepository;
    private final ObjectMapper objectMapper;

    public TaskReplayEngine(
            EventStoreRepositoryPort eventStoreRepository,
            TaskSnapshotRepositoryPort taskSnapshotRepository,
            ObjectMapper objectMapper
    ) {
        this.eventStoreRepository = eventStoreRepository;
        this.taskSnapshotRepository = taskSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    public TaskReplayView replayAt(String tenantId, UUID taskId, Instant replayAt) {
        List<EventStore> replayableEvents = eventStoreRepository.findByAggregateId(tenantId, taskId, TASK_AGGREGATE_TYPE).stream()
                .filter(event -> !event.occurredAt().isAfter(replayAt))
                .toList();
        if (replayableEvents.isEmpty()) {
            throw new PlanningDomainException("Task not found at requested replay point");
        }

        long targetVersion = replayableEvents.get(replayableEvents.size() - 1).version();
        Optional<TaskSnapshot> snapshot = taskSnapshotRepository.findLatestByTaskIdAndTenantIdUpToVersion(taskId, tenantId, targetVersion);
        Task restored = restoreFromSnapshotAndEvents(tenantId, taskId, snapshot, targetVersion);
        if (restored == null) {
            throw new PlanningDomainException("Task was deleted before requested replay point");
        }

        return new TaskReplayView(
                toView(restored),
                replayAt,
                snapshot.map(TaskSnapshot::version).orElse(null),
                targetVersion
        );
    }

    public Task restoreAtVersion(String tenantId, UUID taskId, long targetVersion) {
        Optional<TaskSnapshot> snapshot = taskSnapshotRepository.findLatestByTaskIdAndTenantIdUpToVersion(taskId, tenantId, targetVersion);
        Task restored = restoreFromSnapshotAndEvents(tenantId, taskId, snapshot, targetVersion);
        if (restored == null) {
            throw new PlanningDomainException("Task does not exist at version " + targetVersion);
        }
        return restored;
    }

    private Task restoreFromSnapshotAndEvents(
            String tenantId,
            UUID taskId,
            Optional<TaskSnapshot> snapshot,
            long targetVersion
    ) {
        long startVersionExclusive = snapshot.map(TaskSnapshot::version).orElse(-1L);
        Task current = snapshot.map(TaskSnapshot::toTask).orElse(null);
        List<EventStore> events = eventStoreRepository.findByAggregateIdAfterVersion(
                        tenantId,
                        taskId,
                        TASK_AGGREGATE_TYPE,
                        startVersionExclusive
                ).stream()
                .filter(event -> event.version() <= targetVersion)
                .toList();

        for (EventStore event : events) {
            current = apply(current, event);
        }
        return current;
    }

    private Task apply(Task current, EventStore event) {
        try {
            return switch (event.eventType()) {
                case TaskEventTypes.TASK_CREATED -> fromCreatedEvent(objectMapper.readValue(event.payload(), TaskCreatedEvent.class));
                case TaskEventTypes.TASK_UPDATED -> fromUpdatedEvent(current, objectMapper.readValue(event.payload(), TaskUpdatedEvent.class));
                case TaskEventTypes.TASK_STATUS_CHANGED -> fromStatusChangedEvent(current, objectMapper.readValue(event.payload(), TaskStatusChangedEvent.class));
                case TaskEventTypes.TASK_DELETED -> {
                    objectMapper.readValue(event.payload(), TaskDeletedEvent.class);
                    yield null;
                }
                default -> throw new PlanningDomainException("Unsupported task event type for replay: " + event.eventType());
            };
        } catch (IOException ex) {
            throw new PlanningDomainException("Failed to deserialize task event payload for replay");
        }
    }

    private Task fromCreatedEvent(TaskCreatedEvent event) {
        return new Task(
                event.taskId(),
                event.tenantId(),
                event.sprintId(),
                event.title(),
                event.description(),
                TaskStatus.from(event.status()),
                TaskPriority.from(event.priority()),
                event.occurredAt(),
                event.occurredAt(),
                "system-replay", // Audit field: createdBy
                null,            // Audit field: updatedBy
                null,            // Soft delete: deletedAt
                null,            // Extensibility: metadata
                event.version()
        );
    }

    private Task fromUpdatedEvent(Task current, TaskUpdatedEvent event) {
        if (current == null) {
            throw new PlanningDomainException("Cannot replay update before create event");
        }
        return new Task(
                event.taskId(),
                event.tenantId(),
                event.sprintId(),
                event.title(),
                event.description(),
                current.status(),
                TaskPriority.from(event.priority()),
                current.createdAt(),
                event.occurredAt(),
                current.createdBy(),
                "system-replay", // Audit field: updatedBy
                current.deletedAt(),
                current.metadata(),
                event.version()
        );
    }

    private Task fromStatusChangedEvent(Task current, TaskStatusChangedEvent event) {
        if (current == null) {
            throw new PlanningDomainException("Cannot replay status change before create event");
        }
        return new Task(
                current.id(),
                current.tenantId(),
                current.sprintId(),
                current.title(),
                current.description(),
                TaskStatus.from(event.newStatus()),
                current.priority(),
                current.createdAt(),
                event.occurredAt(),
                current.createdBy(),
                "system-replay", // Audit field: updatedBy
                current.deletedAt(),
                current.metadata(),
                event.version()
        );
    }

    private TaskView toView(Task task) {
        return new TaskView(
                task.id(),
                task.sprintId(),
                task.title(),
                task.description(),
                task.status().name(),
                task.priority().name(),
                task.createdAt(),
                task.tenantId(),
                task.version()
        );
    }
}
