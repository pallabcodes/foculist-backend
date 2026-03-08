package com.yourorg.platform.foculist.gateway.web;

import com.yourorg.platform.foculist.gateway.config.GatewayDownstreamProperties;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import java.time.Duration;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff")
public class BffDashboardController {
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Logger log = LoggerFactory.getLogger(BffDashboardController.class);

    private final GatewayDownstreamProperties downstreamProperties;
    private final WebClient.Builder webClientBuilder;

    public BffDashboardController(GatewayDownstreamProperties downstreamProperties, WebClient.Builder webClientBuilder) {
        this.downstreamProperties = downstreamProperties;
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/dashboard")
    public Mono<ApiResponse<DashboardResponse>> dashboard(
            ServerHttpRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String after
    ) {
        String tenantId = request.getHeaders().getFirst(TENANT_HEADER);
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        String requestId = resolveRequestId(request);

        Mono<Object> projects = fetchList(downstreamProperties.getProject(), "/v1/projects", tenantId, authorization, boundedPage, boundedSize, after);
        Mono<Object> sprints = fetchList(downstreamProperties.getPlanning(), "/v1/sprints", tenantId, authorization, boundedPage, boundedSize, after);
        Mono<Object> bookmarks = fetchList(downstreamProperties.getResource(), "/v1/bookmarks", tenantId, authorization, boundedPage, boundedSize, after);

        return Mono.zip(projects, sprints, bookmarks)
                .map(tuple -> {
                    DashboardResponse payload = new DashboardResponse(
                            tenantId,
                            tuple.getT1(),
                            tuple.getT2(),
                            tuple.getT3(),
                            Instant.now().toString()
                    );
                    return new ApiResponse<>(
                            requestId,
                            "OK",
                            payload,
                            new ApiResponse.Meta(boundedPage, boundedSize, after, false),
                            List.of()
                    );
                });
    }

    private Mono<Object> fetchList(
            String baseUrl,
            String path,
            String tenantId,
            String authorization,
            int page,
            int size,
            String after
    ) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParamIfPresent("after", StringUtils.hasText(after) ? java.util.Optional.of(after) : java.util.Optional.empty())
                        .build()
                )
                .headers(headers -> {
                    headers.set(TENANT_HEADER, tenantId);
                    if (StringUtils.hasText(authorization)) {
                        headers.set(HttpHeaders.AUTHORIZATION, authorization);
                    }
                })
                .retrieve()
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                        .filter(this::isTransient)
                        .maxBackoff(Duration.ofSeconds(2)))
                .doOnError(e -> log.warn("downstream fetch failed path={} tenant={} msg={}", path, tenantId, e.getMessage()))
                .onErrorResume(e -> Mono.just(new DownstreamListResponse<>(
                        "ERROR",
                        List.of(),
                        e.getMessage() != null ? e.getMessage() : "Fetch failed"
                )));
    }

    public record DashboardResponse(
            String tenantId,
            Object projects,
            Object sprints,
            Object bookmarks,
            String generatedAt
    ) {}

    public record DownstreamListResponse<T>(
            String status,
            List<T> data,
            String error
    ) {}

    private boolean isTransient(Throwable throwable) {
        // Simple heuristic: retry on timeouts and IO-type failures
        String message = throwable.getMessage() == null ? "" : throwable.getMessage().toLowerCase();
        return message.contains("timeout") || message.contains("connection") || message.contains("refused");
    }

    private String resolveRequestId(ServerHttpRequest request) {
        String fromHeader = request.getHeaders().getFirst("X-Request-Id");
        if (StringUtils.hasText(fromHeader)) {
            return fromHeader;
        }
        return java.util.UUID.randomUUID().toString();
    }
}
