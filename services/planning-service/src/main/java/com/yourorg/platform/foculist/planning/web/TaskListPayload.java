package com.yourorg.platform.foculist.planning.web;

import com.yourorg.platform.foculist.planning.application.TaskView;
import java.util.List;

public record TaskListPayload(
        List<TaskView> tasks,
        String source
) {}
