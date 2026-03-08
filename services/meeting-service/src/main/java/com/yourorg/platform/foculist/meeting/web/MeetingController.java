package com.yourorg.platform.foculist.meeting.web;

import com.yourorg.platform.foculist.meeting.clean.application.command.CreateMeetingSummaryCommand;
import com.yourorg.platform.foculist.meeting.clean.application.command.ExtractTasksCommand;
import com.yourorg.platform.foculist.meeting.clean.application.service.MeetingApplicationService;
import com.yourorg.platform.foculist.meeting.clean.application.view.ExtractedTaskView;
import com.yourorg.platform.foculist.meeting.clean.application.view.MeetingSummaryView;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/meetings")
@Validated
public class MeetingController {
    private final MeetingApplicationService meetingApplicationService;

    public MeetingController(MeetingApplicationService meetingApplicationService) {
        this.meetingApplicationService = meetingApplicationService;
    }

    @GetMapping("/summaries")
    public List<MeetingSummaryView> listSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return meetingApplicationService.listSummaries(TenantContext.require(), boundedPage, boundedSize);
    }

    @PostMapping("/summaries")
    public ResponseEntity<MeetingSummaryView> createSummary(@Valid @RequestBody CreateSummaryRequest request) {
        MeetingSummaryView created = meetingApplicationService.createSummary(
                TenantContext.require(),
                new CreateMeetingSummaryCommand(request.meetingId(), request.content(), request.style())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/extract-tasks")
    public List<ExtractedTaskView> extractTasks(@Valid @RequestBody ExtractTasksRequest request) {
        return meetingApplicationService.extractTasks(
                TenantContext.require(),
                new ExtractTasksCommand(request.meetingId(), request.transcript())
        );
    }

    public record CreateSummaryRequest(@NotBlank String meetingId, @NotBlank String content, String style) {
    }

    public record ExtractTasksRequest(@NotBlank String meetingId, @NotBlank String transcript) {
    }
}
