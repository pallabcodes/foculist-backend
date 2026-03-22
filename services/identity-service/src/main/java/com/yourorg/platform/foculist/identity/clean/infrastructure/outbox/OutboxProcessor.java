package com.yourorg.platform.foculist.identity.clean.infrastructure.outbox;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OutboxEventRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("OutboxProcessor: Found {} pending events to process", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                event.setStatus("PROCESSING");
                outboxEventRepository.saveAndFlush(event);

                // Enrich payload with tenantId for downstream consumers
                java.util.Map<String, Object> messagePayload = new java.util.HashMap<>(event.getPayload());
                messagePayload.put("tenantId", event.getTenantId());

                // Actual Event Dispatching to RabbitMQ
                if ("WORKSPACE_CREATED".equals(event.getEventType())) {
                    log.info("🚀 [RabbitMQ Dispatch] WORKSPACE_CREATED -> Exchange: 'foculist.workspace.events', Key: 'workspace.created'");
                    rabbitTemplate.convertAndSend("foculist.workspace.events", "workspace.created", messagePayload);
                } else if ("INVITE_CREATED".equals(event.getEventType())) {
                    log.info("📧 [Email Dispatch] INVITE_CREATED -> Simulation only for now");
                }

                event.setStatus("COMPLETED");
                event.setProcessedAt(OffsetDateTime.now());
                outboxEventRepository.save(event);
                log.info("✅ OutboxProcessor: Event {} processed successfully", event.getId());

            } catch (Exception e) {
                log.error("❌ OutboxProcessor: Failed to process event {}", event.getId(), e);
                event.setStatus("FAILED");
                event.setErrorMessage(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }
}
