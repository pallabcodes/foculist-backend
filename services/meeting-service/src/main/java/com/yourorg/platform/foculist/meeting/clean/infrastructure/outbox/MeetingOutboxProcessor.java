package com.yourorg.platform.foculist.meeting.clean.infrastructure.outbox;

import com.yourorg.platform.foculist.meeting.clean.adapter.out.persistence.MeetingOutboxEventJpaRepository;
import com.yourorg.platform.foculist.meeting.clean.adapter.out.persistence.MeetingOutboxEventJpaEntity;
import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingOutboxEventStatus;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingOutboxProcessor {

    private final MeetingOutboxEventJpaRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingEvents() {
        List<MeetingOutboxEventJpaEntity> pendingEvents = repository.findByStatusOrderByOccurredAtAsc(MeetingOutboxEventStatus.PENDING, PageRequest.of(0, 50));
        
        if (pendingEvents.isEmpty()) return;

        log.info("Processing {} pending meeting outbox events", pendingEvents.size());

        for (MeetingOutboxEventJpaEntity event : pendingEvents) {
            Span span = tracer.spanBuilder("process-outbox-event")
                    .setAttribute("event.id", event.getId())
                    .setAttribute("event.type", event.getEventType())
                    .startSpan();

            try (Scope scope = span.makeCurrent()) {
                event.setStatus(MeetingOutboxEventStatus.PROCESSING);
                repository.saveAndFlush(event);

                if ("TASK_PROMOTED".equals(event.getEventType())) {
                    rabbitTemplate.convertAndSend("foculist.meeting.events", "meeting.task.promoted", event.getPayload());
                } else if ("WORKLOG_PROMOTED".equals(event.getEventType())) {
                    rabbitTemplate.convertAndSend("foculist.meeting.events", "meeting.worklog.promoted", event.getPayload());
                }

                event.setStatus(MeetingOutboxEventStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                repository.save(event);
                log.info("Successfully published meeting outbox event: {}", event.getId());
                span.addEvent("published");
            } catch (Exception e) {
                log.error("Failed to process meeting outbox event {}", event.getId(), e);
                event.setStatus(MeetingOutboxEventStatus.FAILED);
                event.setLastError(e.getMessage());
                event.setAttempts(event.getAttempts() + 1);
                repository.save(event);
                span.recordException(e);
            } finally {
                span.end();
            }
        }
    }

    @Scheduled(cron = "0 30 2 * * *") // Daily at 2:30 AM
    @Transactional
    public void purgePublishedEvents() {
        java.time.Instant threshold = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        log.info("MeetingOutboxProcessor: Purging published events older than 7 days (before {})", threshold);
        int deleted = repository.deleteByStatusAndOccurredAtBefore(MeetingOutboxEventStatus.PUBLISHED, threshold);
        if (deleted > 0) {
            log.info("MeetingOutboxProcessor: Successfully purged {} published events", deleted);
        }
    }
}
