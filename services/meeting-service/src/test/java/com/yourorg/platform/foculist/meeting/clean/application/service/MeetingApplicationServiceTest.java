package com.yourorg.platform.foculist.meeting.clean.application.service;

import com.yourorg.platform.foculist.meeting.clean.application.command.CreateMeetingSummaryCommand;
import com.yourorg.platform.foculist.meeting.clean.application.command.ExtractTasksCommand;
import com.yourorg.platform.foculist.meeting.clean.application.view.ExtractedTaskView;
import com.yourorg.platform.foculist.meeting.clean.application.view.MeetingSummaryView;
import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingDomainException;
import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingSummary;
import com.yourorg.platform.foculist.meeting.clean.domain.model.SummaryStyle;
import com.yourorg.platform.foculist.meeting.clean.domain.port.MeetingSummaryRepositoryPort;
import com.yourorg.platform.foculist.meeting.clean.domain.port.PlanningClientPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeetingApplicationServiceTest {

    @Test
    void createsSummary() {
        MeetingSummaryRepositoryPort summaryRepository = mock(MeetingSummaryRepositoryPort.class);
        PlanningClientPort planningClient = mock(PlanningClientPort.class);
        MeetingApplicationService service = new MeetingApplicationService(summaryRepository, planningClient);
        when(summaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingSummaryView created = service.createSummary(
                "tenant-a",
                new CreateMeetingSummaryCommand("meeting-1", "Sprint goals aligned", "action_focused")
        );

        assertThat(created.meetingId()).isEqualTo("meeting-1");
        assertThat(created.style()).isEqualTo("ACTION_FOCUSED");
        assertThat(created.tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void listsSummariesForTenant() {
        MeetingSummaryRepositoryPort summaryRepository = mock(MeetingSummaryRepositoryPort.class);
        PlanningClientPort planningClient = mock(PlanningClientPort.class);
        MeetingApplicationService service = new MeetingApplicationService(summaryRepository, planningClient);
        MeetingSummary summary = MeetingSummary.create(
                "tenant-a",
                "meeting-2",
                "Decisions captured",
                SummaryStyle.CONCISE,
                Instant.parse("2026-02-04T10:00:00Z")
        );
        when(summaryRepository.findByTenantId("tenant-a", 0, 50)).thenReturn(List.of(summary));

        List<MeetingSummaryView> summaries = service.listSummaries("tenant-a", 0, 50);
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).meetingId()).isEqualTo("meeting-2");
    }

    @Test
    void rejectsInvalidSummaryStyle() {
        MeetingSummaryRepositoryPort summaryRepository = mock(MeetingSummaryRepositoryPort.class);
        PlanningClientPort planningClient = mock(PlanningClientPort.class);
        MeetingApplicationService service = new MeetingApplicationService(summaryRepository, planningClient);

        assertThatThrownBy(() -> service.createSummary(
                "tenant-a",
                new CreateMeetingSummaryCommand("meeting-1", "content", "invalid-style")
        )).isInstanceOf(MeetingDomainException.class).hasMessageContaining("Invalid summary style");
    }

    @Test
    void extractsTasksFromTranscriptSignals() {
        MeetingSummaryRepositoryPort summaryRepository = mock(MeetingSummaryRepositoryPort.class);
        PlanningClientPort planningClient = mock(PlanningClientPort.class);
        MeetingApplicationService service = new MeetingApplicationService(summaryRepository, planningClient);

        List<ExtractedTaskView> tasks = service.extractTasks(
                "tenant-a",
                new ExtractTasksCommand(
                        "meeting-9",
                        "Action: assign owner for API gateway hardening.\nNeed to close blocker in auth flow ASAP.\nMinor cleanup can happen later."
                )
        );

        assertThat(tasks).isNotEmpty();
        assertThat(tasks.get(0).sourceMeetingId()).isEqualTo("meeting-9");
        assertThat(tasks.stream().map(ExtractedTaskView::priority))
                .contains("HIGH")
                .contains("LOW");
    }

    @Test
    void returnsFallbackTaskWhenTranscriptHasNoSignals() {
        MeetingSummaryRepositoryPort summaryRepository = mock(MeetingSummaryRepositoryPort.class);
        PlanningClientPort planningClient = mock(PlanningClientPort.class);
        MeetingApplicationService service = new MeetingApplicationService(summaryRepository, planningClient);

        List<ExtractedTaskView> tasks = service.extractTasks(
                "tenant-a",
                new ExtractTasksCommand("meeting-3", "General discussion with no explicit actions.")
        );

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).title()).contains("Review meeting notes");
    }

    @Test
    void promotesTaskToPlanning() {
        MeetingSummaryRepositoryPort summaryRepository = mock(MeetingSummaryRepositoryPort.class);
        PlanningClientPort planningClient = mock(PlanningClientPort.class);
        MeetingApplicationService service = new MeetingApplicationService(summaryRepository, planningClient);

        service.promoteTask("tenant-a", "meeting-1", "Fix bug", "HIGH");

        org.mockito.Mockito.verify(planningClient).createTask(
                org.mockito.ArgumentMatchers.eq("tenant-a"),
                org.mockito.ArgumentMatchers.eq("Fix bug"),
                org.mockito.ArgumentMatchers.contains("meeting-1"),
                org.mockito.ArgumentMatchers.eq(com.yourorg.platform.foculist.meeting.clean.domain.model.TaskPriority.HIGH)
        );
    }
}
