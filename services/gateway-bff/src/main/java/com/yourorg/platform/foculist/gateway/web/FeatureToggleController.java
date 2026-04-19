package com.yourorg.platform.foculist.gateway.web;

import com.yourorg.platform.foculist.tenancy.feature.FeatureToggleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureToggleController {

    private final FeatureToggleService featureToggleService;

    public FeatureToggleController(FeatureToggleService featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    @GetMapping
    public Mono<Map<String, Boolean>> getFeatures(ServerWebExchange exchange) {
        // Evaluate core features for the current tenant
        // In a real environment, this would list all active flags from Unleash
        return Mono.just(Map.of(
            "ai-enrichment", featureToggleService.isEnabled("ai-enrichment"),
            "sync-v2", featureToggleService.isEnabled("sync-v2", true),
            "multi-workspace", true
        ));
    }
}
