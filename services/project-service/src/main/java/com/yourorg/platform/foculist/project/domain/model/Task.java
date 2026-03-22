package com.yourorg.platform.foculist.project.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Task {
    private final UUID id;
    private final String tenantId;
    private final UUID projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String type;
    private Integer storyPoints;
    private UUID assigneeId;
    private final UUID reporterId;
    private LocalDate dueDate;
    private final Instant createdAt;
    private Instant updatedAt;
    private Long version;
    private Map<String, Object> metadata;

    public static Task create(
            String tenantId,
            UUID projectId,
            String title,
            String description,
            String status,
            String priority,
            String type,
            Integer storyPoints,
            UUID assigneeId,
            UUID reporterId,
            LocalDate dueDate,
            Instant now
    ) {
        validateTitle(title);
        return Task.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .projectId(projectId)
                .title(title)
                .description(description)
                .status(status != null ? status : "BACKLOG")
                .priority(priority != null ? priority : "MEDIUM")
                .type(type != null ? type : "TASK")
                .storyPoints(storyPoints)
                .assigneeId(assigneeId)
                .reporterId(reporterId)
                .dueDate(dueDate)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .metadata(Map.of())
                .build();
    }

    public void update(
            String title,
            String description,
            String status,
            String priority,
            String type,
            Integer storyPoints,
            UUID assigneeId,
            LocalDate dueDate,
            Instant now
    ) {
        if (title != null) {
            validateTitle(title);
            this.title = title;
        }
        if (description != null) this.description = description;
        if (status != null) this.status = status;
        if (priority != null) this.priority = priority;
        if (type != null) this.type = type;
        this.storyPoints = storyPoints;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
        this.updatedAt = now;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ProjectDomainException("Task title cannot be empty");
        }
        if (title.length() > 512) {
            throw new ProjectDomainException("Task title is too long");
        }
    }
}
