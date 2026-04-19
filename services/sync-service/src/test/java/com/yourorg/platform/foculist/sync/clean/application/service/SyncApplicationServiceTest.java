package com.yourorg.platform.foculist.sync.clean.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPullCommand;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPushCommand;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPullResponseView;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPushResponseView;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncChangeEvent;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDeviceCursor;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDomainException;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncChangeEventRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncDeviceCursorRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncPushEnvelopeRepositoryPort;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncRealtimeOpLogRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncApplicationServiceTest {

    @Test
    void pushPersistsEnvelopeEventAndCursor() {
        Instant now = Instant.parse("2026-02-21T10:15:00Z");
        SyncPushEnvelopeRepositoryPort pushEnvelopeRepository = mock(SyncPushEnvelopeRepositoryPort.class);
        SyncChangeEventRepositoryPort changeEventRepository = mock(SyncChangeEventRepositoryPort.class);
        SyncDeviceCursorRepositoryPort deviceCursorRepository = mock(SyncDeviceCursorRepositoryPort.class);
        SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository = mock(SyncRealtimeOpLogRepositoryPort.class);
        SyncApplicationService service = new SyncApplicationService(
                pushEnvelopeRepository,
                changeEventRepository,
                deviceCursorRepository,
                realtimeOpLogRepository,
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        when(pushEnvelopeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(changeEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceCursorRepository.findByTenantIdAndDeviceId("tenant-a", "device-1")).thenReturn(Optional.empty());
        when(deviceCursorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncPushResponseView response = service.push(
                "tenant-a",
                new SyncPushCommand(
                        "device-1",
                        3,
                        null,
                        Map.of("projects", 2),
                        "2026-02-21T10:14:00Z"
                )
        );

        assertThat(response.accepted()).isTrue();
        assertThat(response.deviceId()).isEqualTo("device-1");
        assertThat(response.pendingChanges()).isEqualTo(3);
        assertThat(response.tenantId()).isEqualTo("tenant-a");
        verify(changeEventRepository).save(any());
        verify(deviceCursorRepository).save(any());
    }

    @Test
    void pushSkipsChangeEventWhenPendingChangesIsZero() {
        Instant now = Instant.parse("2026-02-21T10:20:00Z");
        SyncPushEnvelopeRepositoryPort pushEnvelopeRepository = mock(SyncPushEnvelopeRepositoryPort.class);
        SyncChangeEventRepositoryPort changeEventRepository = mock(SyncChangeEventRepositoryPort.class);
        SyncDeviceCursorRepositoryPort deviceCursorRepository = mock(SyncDeviceCursorRepositoryPort.class);
        SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository = mock(SyncRealtimeOpLogRepositoryPort.class);
        SyncApplicationService service = new SyncApplicationService(
                pushEnvelopeRepository,
                changeEventRepository,
                deviceCursorRepository,
                realtimeOpLogRepository,
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        when(pushEnvelopeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceCursorRepository.findByTenantIdAndDeviceId("tenant-a", "device-1")).thenReturn(Optional.empty());
        when(deviceCursorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncPushResponseView response = service.push(
                "tenant-a",
                new SyncPushCommand("device-1", 0, "v2", Map.of(), null)
        );

        assertThat(response.pendingChanges()).isEqualTo(0);
        verify(changeEventRepository, never()).save(any());
    }

    @Test
    void pullReturnsTenantScopedChangesAndUpdatesCursor() {
        Instant now = Instant.parse("2026-02-21T10:30:00Z");
        SyncPushEnvelopeRepositoryPort pushEnvelopeRepository = mock(SyncPushEnvelopeRepositoryPort.class);
        SyncChangeEventRepositoryPort changeEventRepository = mock(SyncChangeEventRepositoryPort.class);
        SyncDeviceCursorRepositoryPort deviceCursorRepository = mock(SyncDeviceCursorRepositoryPort.class);
        SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository = mock(SyncRealtimeOpLogRepositoryPort.class);
        SyncApplicationService service = new SyncApplicationService(
                pushEnvelopeRepository,
                changeEventRepository,
                deviceCursorRepository,
                realtimeOpLogRepository,
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        Instant lastSync = Instant.parse("2026-02-21T10:00:00Z");
        SyncChangeEvent event = SyncChangeEvent.batch(
                "tenant-a",
                "device-2",
                "{\"kind\":\"TASK_UPDATED\"}",
                now.minusSeconds(60)
        );
        SyncDeviceCursor existingCursor = SyncDeviceCursor.create(
                "tenant-a",
                "device-2",
                lastSync,
                now.minusSeconds(120)
        );

        when(changeEventRepository.findSince("tenant-a", lastSync, 200)).thenReturn(List.of(event));
        when(deviceCursorRepository.findByTenantIdAndDeviceId("tenant-a", "device-2")).thenReturn(Optional.of(existingCursor));
        when(deviceCursorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncPullResponseView response = service.pull(
                "tenant-a",
                new SyncPullCommand("device-2", "2026-02-21T10:00:00Z")
        );

        assertThat(response.changeCount()).isEqualTo(1);
        assertThat(response.changes()).hasSize(1);
        assertThat(response.changes().get(0).type()).isEqualTo("BATCH");
        assertThat(response.tenantId()).isEqualTo("tenant-a");
        verify(changeEventRepository).findSince(eq("tenant-a"), eq(lastSync), eq(200));
        verify(deviceCursorRepository).save(any());
    }

    @Test
    void rejectsInvalidPullCursorFormat() {
        SyncPushEnvelopeRepositoryPort pushEnvelopeRepository = mock(SyncPushEnvelopeRepositoryPort.class);
        SyncChangeEventRepositoryPort changeEventRepository = mock(SyncChangeEventRepositoryPort.class);
        SyncDeviceCursorRepositoryPort deviceCursorRepository = mock(SyncDeviceCursorRepositoryPort.class);
        SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository = mock(SyncRealtimeOpLogRepositoryPort.class);
        SyncApplicationService service = new SyncApplicationService(
                pushEnvelopeRepository,
                changeEventRepository,
                deviceCursorRepository,
                realtimeOpLogRepository,
                new ObjectMapper()
        );

        assertThatThrownBy(() -> service.pull("tenant-a", new SyncPullCommand("device-1", "21-02-2026")))
                .isInstanceOf(SyncDomainException.class)
                .hasMessageContaining("Invalid lastSync format");
    }

    @Test
    void rejectsInvalidClientSyncFormatOnPush() {
        SyncPushEnvelopeRepositoryPort pushEnvelopeRepository = mock(SyncPushEnvelopeRepositoryPort.class);
        SyncChangeEventRepositoryPort changeEventRepository = mock(SyncChangeEventRepositoryPort.class);
        SyncDeviceCursorRepositoryPort deviceCursorRepository = mock(SyncDeviceCursorRepositoryPort.class);
        SyncRealtimeOpLogRepositoryPort realtimeOpLogRepository = mock(SyncRealtimeOpLogRepositoryPort.class);
        SyncApplicationService service = new SyncApplicationService(
                pushEnvelopeRepository,
                changeEventRepository,
                deviceCursorRepository,
                realtimeOpLogRepository,
                new ObjectMapper()
        );

        assertThatThrownBy(() -> service.push(
                "tenant-a",
                new SyncPushCommand("device-1", 1, "v1", Map.of(), "invalid-time")
        )).isInstanceOf(SyncDomainException.class).hasMessageContaining("Invalid clientSyncTime format");
    }
}
