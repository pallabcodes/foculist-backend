package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import com.yourorg.platform.foculist.planning.application.PlanningApplicationService;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.outbox", name = "polling-enabled", havingValue = "true")
public class PlanningOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(PlanningOutboxPublisher.class);

    private final OutboxEventRepositoryPort outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.outbox.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.outbox.task-created-topic:planning.task.created.v1}")
    private String taskCreatedTopic;

    public PlanningOutboxPublisher(
            OutboxEventRepositoryPort outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this(outboxEventRepository, kafkaTemplate, Clock.systemUTC());
    }

    PlanningOutboxPublisher(
            OutboxEventRepositoryPort outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.claimPendingBatch(batchSize, maxAttempts);
        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void recoverStuckProcessingEvents() {
        int count = outboxEventRepository.resetStuckProcessingEvents();
        if (count > 0) {
            log.info("Recovered {} stuck outbox events on application startup", count);
        }
    }

    private void publishEvent(OutboxEvent event) {
        String topic = resolveTopic(event.eventType());
        String key = event.tenantId() + ":" + event.aggregateId();
        try {
            kafkaTemplate.send(topic, key, event.payload()).get(5, TimeUnit.SECONDS);
            outboxEventRepository.markPublished(event.id(), Instant.now(clock));
            log.info("Published outbox event id={} eventType={} topic={}", event.id(), event.eventType(), topic);
        } catch (Exception e) {
            outboxEventRepository.markFailed(event.id(), truncateError(e.getMessage()));
            log.warn("Failed publishing outbox event id={} eventType={} topic={} error={}",
                    event.id(),
                    event.eventType(),
                    topic,
                    e.toString());
        }
    }

    private String resolveTopic(String eventType) {
        if (PlanningApplicationService.TASK_CREATED_EVENT_TYPE.equals(eventType)) {
            return taskCreatedTopic;
        }
        return taskCreatedTopic;
    }

    private String truncateError(String error) {
        if (error == null) {
            return "publish failure";
        }
        int maxLength = 1024;
        if (error.length() <= maxLength) {
            return error;
        }
        return error.substring(0, maxLength);
    }
}
