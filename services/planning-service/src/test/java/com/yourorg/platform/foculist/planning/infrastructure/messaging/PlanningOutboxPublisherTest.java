package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import com.yourorg.platform.foculist.planning.application.PlanningApplicationService;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PlanningOutboxPublisherTest {

    @Test
    void marksPublishedWhenKafkaSendSucceeds() {
        OutboxEventRepositoryPort repository = mock(OutboxEventRepositoryPort.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        Instant now = Instant.parse("2026-02-15T10:00:00Z");

        OutboxEvent event = OutboxEvent.newEvent(
                "tenant-a",
                "Task",
                UUID.randomUUID(),
                PlanningApplicationService.TASK_CREATED_EVENT_TYPE,
                "{\"a\":1}",
                now.minusSeconds(1)
        );

        when(repository.claimPendingBatch(10, 3)).thenReturn(List.of(event));
        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> successFuture = new CompletableFuture<>();
        successFuture.complete(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(successFuture);

        PlanningOutboxPublisher publisher = new PlanningOutboxPublisher(repository, kafkaTemplate, Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(publisher, "batchSize", 10);
        ReflectionTestUtils.setField(publisher, "maxAttempts", 3);
        ReflectionTestUtils.setField(publisher, "taskCreatedTopic", "planning.task.created.v1");

        publisher.publishPendingEvents();

        verify(repository).markPublished(eq(event.id()), eq(now));
    }

    @Test
    void marksFailedWhenKafkaSendFails() {
        OutboxEventRepositoryPort repository = mock(OutboxEventRepositoryPort.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        Instant now = Instant.parse("2026-02-15T10:00:00Z");

        OutboxEvent event = OutboxEvent.newEvent(
                "tenant-a",
                "Task",
                UUID.randomUUID(),
                PlanningApplicationService.TASK_CREATED_EVENT_TYPE,
                "{\"a\":1}",
                now.minusSeconds(1)
        );

        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker down"));

        when(repository.claimPendingBatch(10, 3)).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class))).thenReturn(failedFuture);

        PlanningOutboxPublisher publisher = new PlanningOutboxPublisher(repository, kafkaTemplate, Clock.fixed(now, ZoneOffset.UTC));
        ReflectionTestUtils.setField(publisher, "batchSize", 10);
        ReflectionTestUtils.setField(publisher, "maxAttempts", 3);
        ReflectionTestUtils.setField(publisher, "taskCreatedTopic", "planning.task.created.v1");

        publisher.publishPendingEvents();

        verify(repository).markFailed(eq(event.id()), any(String.class));
    }
}
