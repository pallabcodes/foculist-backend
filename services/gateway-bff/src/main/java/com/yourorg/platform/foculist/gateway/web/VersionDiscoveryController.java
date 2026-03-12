package com.yourorg.platform.foculist.gateway.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/versions")
public class VersionDiscoveryController {

    @GetMapping
    public Mono<Map<String, Object>> getVersions() {
        return Mono.just(Map.of(
            "current", "v1",
            "available", List.of(
                Map.of("version", "v1", "status", "STABLE", "documentation", "https://docs.foculist.com/api/v1"),
                Map.of("version", "v2", "status", "BETA", "documentation", "https://docs.foculist.com/api/v2")
            ),
            "deprecation_policy", "https://docs.foculist.com/api/deprecation"
        ));
    }
}
