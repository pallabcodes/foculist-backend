package com.yourorg.platform.foculist.planning.domain.event;

public final class TaskEventTypes {
    public static final String TASK_CREATED = "planning.task.created.v1";
    public static final String TASK_UPDATED = "planning.task.updated.v1";
    public static final String TASK_STATUS_CHANGED = "planning.task.status-changed.v1";
    public static final String TASK_DELETED = "planning.task.deleted.v1";

    private TaskEventTypes() {
    }
}
