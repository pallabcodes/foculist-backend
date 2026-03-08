package com.yourorg.platform.foculist.planning.web;

import com.yourorg.platform.foculist.planning.application.TaskView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("taskResponseMapperV2")
public class V2TaskResponseMapper implements TaskResponseMapper {
    @Override
    public ApiResponse<TaskListPayload> toResponse(
            List<TaskView> tasks,
            int page,
            int size,
            String nextCursor,
            boolean hasMore,
            String requestId
    ) {
        TaskListPayload payload = new TaskListPayload(tasks, "planning-service");
        var meta = new ApiResponse.Meta(page, size, nextCursor, hasMore);
        return ApiResponse.success(requestId, payload, meta);
    }
}
