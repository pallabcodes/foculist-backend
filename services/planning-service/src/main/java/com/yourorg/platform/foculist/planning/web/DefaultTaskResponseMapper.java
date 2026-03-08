package com.yourorg.platform.foculist.planning.web;

import com.yourorg.platform.foculist.planning.application.TaskView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("taskResponseMapperV1")
public class DefaultTaskResponseMapper implements TaskResponseMapper {
    @Override
    public ApiResponse<List<TaskView>> toResponse(
            List<TaskView> tasks,
            int page,
            int size,
            String nextCursor,
            boolean hasMore,
            String requestId
    ) {
        var meta = new ApiResponse.Meta(page, size, nextCursor, hasMore);
        return ApiResponse.success(requestId, tasks, meta);
    }
}
