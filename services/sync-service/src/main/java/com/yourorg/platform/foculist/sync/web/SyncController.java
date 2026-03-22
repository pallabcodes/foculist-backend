package com.yourorg.platform.foculist.sync.web;

import com.yourorg.platform.foculist.sync.clean.application.command.SyncPullCommand;
import com.yourorg.platform.foculist.sync.clean.application.command.SyncPushCommand;
import com.yourorg.platform.foculist.sync.clean.application.service.SyncApplicationService;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPullResponseView;
import com.yourorg.platform.foculist.sync.clean.application.view.SyncPushResponseView;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sync")
@Validated
public class SyncController {
    private final SyncApplicationService syncApplicationService;

    public SyncController(SyncApplicationService syncApplicationService) {
        this.syncApplicationService = syncApplicationService;
    }

    @PostMapping("/push")
    public SyncPushResponseView push(
            @Valid @RequestBody SyncPushRequest request,
            org.springframework.web.server.ServerWebExchange exchange
    ) {
        String tenantId = exchange.getAttribute("tenantId");
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Tenant identifier is missing");
        }
        return syncApplicationService.push(
                tenantId,
                new SyncPushCommand(
                        request.deviceId(),
                        request.pendingChanges(),
                        request.payloadVersion(),
                        request.payload(),
                        request.clientSyncTime()
                )
        );
    }

    @PostMapping("/pull")
    public SyncPullResponseView pull(
            @Valid @RequestBody SyncPullRequest request,
            org.springframework.web.server.ServerWebExchange exchange
    ) {
        String tenantId = exchange.getAttribute("tenantId");
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Tenant identifier is missing");
        }
        return syncApplicationService.pull(
                tenantId,
                new SyncPullCommand(
                        request.deviceId(),
                        request.lastSync()
                )
        );
    }

    public record SyncPushRequest(
            @NotBlank String deviceId,
            int pendingChanges,
            String payloadVersion,
            Map<String, Object> payload,
            String clientSyncTime
    ) {
    }

    public record SyncPullRequest(
            @NotBlank String deviceId,
            String lastSync
    ) {
    }
}
