package com.yourorg.platform.foculist.planning.web;

import com.yourorg.platform.foculist.planning.application.TaskView;
import java.util.List;

public interface TaskResponseMapper {
    ApiResponse<?> toResponse(
            List<TaskView> tasks,
            int page,
            int size,
            String nextCursor,
            boolean hasMore,
            String requestId
    );
}
