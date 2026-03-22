package com.yourorg.platform.foculist.meeting.clean.application.service;

import com.yourorg.platform.foculist.meeting.clean.application.command.CreateMeetingSummaryCommand;
import com.yourorg.platform.foculist.meeting.clean.application.command.ExtractTasksCommand;
import com.yourorg.platform.foculist.meeting.clean.application.view.ExtractedTaskView;
import com.yourorg.platform.foculist.meeting.clean.application.view.MeetingSummaryView;
import com.yourorg.platform.foculist.meeting.clean.domain.model.ExtractedTask;
import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingDomainException;
import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingSummary;
import com.yourorg.platform.foculist.meeting.clean.domain.model.SummaryStyle;
import com.yourorg.platform.foculist.meeting.clean.domain.model.TaskPriority;
import com.yourorg.platform.foculist.meeting.clean.domain.port.MeetingSummaryRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingApplicationService {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\n\\r\\.]+");

    private final MeetingSummaryRepositoryPort summaryRepository;
    private final Clock clock;

    public MeetingApplicationService(MeetingSummaryRepositoryPort summaryRepository) {
        this.summaryRepository = summaryRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<MeetingSummaryView> listSummaries(String tenantId, int page, int size) {
        return summaryRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toSummaryView)
                .toList();
    }

    @Transactional
    public MeetingSummaryView createSummary(String tenantId, CreateMeetingSummaryCommand command) {
        MeetingSummary created = summaryRepository.save(MeetingSummary.create(
                tenantId,
                command.meetingId(),
                command.content(),
                SummaryStyle.from(command.style()),
                Instant.now(clock)
        ));
        return toSummaryView(created);
    }

    @Transactional(readOnly = true)
    public List<ExtractedTaskView> extractTasks(String tenantId, ExtractTasksCommand command) {
        if (command.transcript() == null || command.transcript().isBlank()) {
            throw new MeetingDomainException("transcript is required");
        }
        List<ExtractedTask> tasks = deriveTasks(tenantId, command.meetingId(), command.transcript());
        return tasks.stream().map(this::toTaskView).toList();
    }

    private List<ExtractedTask> deriveTasks(String tenantId, String meetingId, String transcript) {
        List<String> candidates = extractTaskCandidates(transcript);
        if (candidates.isEmpty()) {
            candidates = List.of("Review meeting notes and confirm follow-up actions");
        }

        List<ExtractedTask> tasks = new ArrayList<>();
        for (String candidate : candidates) {
            tasks.add(new ExtractedTask(
                    normalizeTaskTitle(candidate),
                    inferPriority(candidate),
                    meetingId,
                    tenantId
            ));
        }
        return tasks;
    }

    private List<String> extractTaskCandidates(String transcript) {
        String[] parts = SPLIT_PATTERN.split(transcript);
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : parts) {
            String line = raw == null ? "" : raw.trim();
            if (line.isBlank()) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (containsActionSignal(lower)) {
                unique.add(line);
            }
            if (unique.size() >= 10) {
                break;
            }
        }
        return List.copyOf(unique);
    }

    private boolean containsActionSignal(String lower) {
        return lower.contains("action item")
                || lower.startsWith("action:")
                || lower.contains("follow up")
                || lower.contains("todo")
                || lower.contains("to do")
                || lower.contains("need to")
                || lower.contains("should")
                || lower.contains("assign")
                || lower.contains("owner")
                || lower.contains("cleanup");
    }

    private String normalizeTaskTitle(String raw) {
        String sanitized = raw.replaceAll("\\s+", " ").trim();
        if (sanitized.length() <= 140) {
            return sanitized;
        }
        return sanitized.substring(0, 140).trim();
    }

    private TaskPriority inferPriority(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("critical")) {
            return TaskPriority.CRITICAL;
        }
        if (lower.contains("urgent") || lower.contains("asap") || lower.contains("blocker")) {
            return TaskPriority.HIGH;
        }
        if (lower.contains("minor") || lower.contains("later") || lower.contains("low")) {
            return TaskPriority.LOW;
        }
        return TaskPriority.MEDIUM;
    }

    private MeetingSummaryView toSummaryView(MeetingSummary summary) {
        return new MeetingSummaryView(
                summary.id(),
                summary.meetingId(),
                summary.content(),
                summary.style().name(),
                summary.tenantId(),
                summary.createdAt()
        );
    }

    private ExtractedTaskView toTaskView(ExtractedTask extractedTask) {
        return new ExtractedTaskView(
                extractedTask.title(),
                extractedTask.priority().name(),
                extractedTask.sourceMeetingId(),
                extractedTask.tenantId()
        );
    }
}
