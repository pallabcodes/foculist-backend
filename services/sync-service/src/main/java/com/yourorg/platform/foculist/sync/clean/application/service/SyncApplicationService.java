package com.yourorg.platform.foculist.sync.clean.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPullCommand;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPushCommand;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncChangeView;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPullResponseView;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPushResponseView;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncChangeEvent;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDeviceCursor;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDomainException;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncPushEnvelope;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncChangeEventRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncDeviceCursorRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncPushEnvelopeRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncRealtimeOpLogRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncApplicationService {
    private static final int PULL_LIMIT = 200;

    private final SyncPushEnvelopeRepositoryPort pushEnvelopeRepository;
    private final SyncChangeEventRepositoryPort changeEventRepository;
    private final SyncDeviceCursorRepositoryPort deviceCursorRepository;
    private final SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SyncApplicationService(
            SyncPushEnvelopeRepositoryPort pushEnvelopeRepository,
            SyncChangeEventRepositoryPort changeEventRepository,
            SyncDeviceCursorRepositoryPort deviceCursorRepository,
            SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository,
            ObjectMapper objectMapper
    ) {
        this(
                pushEnvelopeRepository,
                changeEventRepository,
                deviceCursorRepository,
                realtimeOpLogRepository,
                objectMapper,
                Clock.systemUTC()
        );
    }

    SyncApplicationService(
            SyncPushEnvelopeRepositoryPort pushEnvelopeRepository,
            SyncChangeEventRepositoryPort changeEventRepository,
            SyncDeviceCursorRepositoryPort deviceCursorRepository,
            SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.pushEnvelopeRepository = pushEnvelopeRepository;
        this.changeEventRepository = changeEventRepository;
        this.deviceCursorRepository = deviceCursorRepository;
        this.realtimeOpLogRepository = realtimeOpLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public SyncPushResponseView push(String tenantId, SyncPushCommand command) {
        Instant now = Instant.now(clock);
        String payloadJson = toJson(command.payload());
        Instant clientSync = parseOptionalInstant(command.clientSyncTime(), "clientSyncTime");

        SyncPushEnvelope savedEnvelope = pushEnvelopeRepository.save(SyncPushEnvelope.create(
                tenantId,
                command.deviceId(),
                resolvePayloadVersion(command.payloadVersion()),
                command.pendingChanges(),
                payloadJson,
                clientSync,
                now
        ));

        // Offload real-time deltas to NoSQL Op-Log for infinite horizontal scale
        if ("realtime".equalsIgnoreCase(command.payloadVersion()) || command.payload().containsKey("realtime")) {
            realtimeOpLogRepository.append(com.yourorg.platform.foculist.sync.clean.domain.model.SyncRealtimeOpLogEntry.create(
                    tenantId,
                    (String) command.payload().getOrDefault("projectId", "default"),
                    command.deviceId(),
                    (String) command.payload().getOrDefault("destination", "broadcast"),
                    payloadJson,
                    now,
                    now.plus(24, java.time.temporal.ChronoUnit.HOURS) // 24-hour TTL for high-frequency deltas
            ));
        }

        if (savedEnvelope.pendingChanges() > 0) {
            changeEventRepository.save(SyncChangeEvent.batch(
                    tenantId,
                    savedEnvelope.deviceId(),
                    payloadJson,
                    now
            ));
        }

        SyncDeviceCursor cursor = deviceCursorRepository.findByTenantIdAndDeviceId(tenantId, savedEnvelope.deviceId())
                .map(existing -> existing.touch(clientSync, now))
                .orElseGet(() -> SyncDeviceCursor.create(tenantId, savedEnvelope.deviceId(), clientSync, now));
        deviceCursorRepository.save(cursor);

        return new SyncPushResponseView(
                true,
                savedEnvelope.id(),
                savedEnvelope.deviceId(),
                savedEnvelope.pendingChanges(),
                savedEnvelope.receivedAt(),
                tenantId
        );
    }

    @Transactional
    public SyncPullResponseView pull(String tenantId, SyncPullCommand command) {
        Instant since = parseOptionalInstant(command.lastSync(), "lastSync");
        Instant now = Instant.now(clock);
        Instant cursorSince = since == null ? Instant.EPOCH : since;

        List<SyncChangeView> changes = changeEventRepository.findSince(tenantId, cursorSince, PULL_LIMIT).stream()
                .map(event -> new SyncChangeView(
                        event.id(),
                        event.changeType().name(),
                        event.deviceId(),
                        event.payload(),
                        event.occurredAt()
                ))
                .toList();

        SyncDeviceCursor updatedCursor = deviceCursorRepository.findByTenantIdAndDeviceId(tenantId, command.deviceId())
                .map(existing -> existing.touch(since, now))
                .orElseGet(() -> SyncDeviceCursor.create(tenantId, command.deviceId(), since, now));
        deviceCursorRepository.save(updatedCursor);

        Instant newCursor = now;
        if (!changes.isEmpty()) {
            Instant lastEventTime = changes.get(changes.size() - 1).eventTime();
            if (changes.size() == PULL_LIMIT || lastEventTime.isAfter(now)) {
                newCursor = lastEventTime;
            }
        }

        return new SyncPullResponseView(
                command.lastSync(),
                changes,
                changes.size(),
                now,
                newCursor,
                tenantId
        );
    }

    private String resolvePayloadVersion(String payloadVersion) {
        if (payloadVersion == null || payloadVersion.isBlank()) {
            return "v1";
        }
        return payloadVersion.trim();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            throw new SyncDomainException("Failed to serialize payload");
        }
    }

    private Instant parseOptionalInstant(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new SyncDomainException("Invalid " + fieldName + " format. Expected ISO-8601 instant");
        }
    }
}
