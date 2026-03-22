package com.yourorg.platform.foculist.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayDownstreamProperties.class)
public class GatewayRoutingConfig {

    @Bean
    RouteLocator foculistRoutes(RouteLocatorBuilder builder, GatewayDownstreamProperties downstream) {
        return builder.routes()
                // Internal OpenAPI routes for aggregation - MUST BE FIRST for precedence
                .route("identity-docs", route -> route
                        .path("/api/identity/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getIdentity()))
                .route("planning-docs", route -> route
                        .path("/api/planning/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getPlanning()))
                .route("project-docs", route -> route
                        .path("/api/project/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getProject()))
                .route("sync-docs", route -> route
                        .path("/api/sync/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getSync()))
                .route("calendar-docs", route -> route
                        .path("/api/calendar/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getCalendar()))
                .route("meeting-docs", route -> route
                        .path("/api/meetings/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getMeeting()))
                .route("resource-docs", route -> route
                        .path("/api/resources/v3/api-docs")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(downstream.getResource()))
                // API Routes
                .route("identity-auth-v2", route -> route
                        .path("/api/auth/**")
                        .and().header("X-API-Version", "2")
                        .filters(filter -> filter.rewritePath("/api/auth(?<segment>/?.*)", "/v2/auth${segment}"))
                        .uri(downstream.getIdentity()))
                .route("identity-auth-v1", route -> route
                        .path("/api/auth/**")
                        .filters(filter -> filter.rewritePath("/api/auth(?<segment>/?.*)", "/v1/auth${segment}"))
                        .uri(downstream.getIdentity()))
                .route("identity-user-compat", route -> route
                        .path("/api/user", "/api/user/**")
                        .filters(filter -> filter.rewritePath("/api/user(?<segment>/?.*)", "/v1/user${segment}"))
                        .uri(downstream.getIdentity()))
                .route("identity-users-v2", route -> route
                        .path("/api/users/**")
                        .and().header("X-API-Version", "2")
                        .filters(filter -> filter.rewritePath("/api/users(?<segment>/?.*)", "/v2/users${segment}"))
                        .uri(downstream.getIdentity()))
                .route("identity-users-v1", route -> route
                        .path("/api/users/**")
                        .filters(filter -> filter.rewritePath("/api/users(?<segment>/?.*)", "/v1/users${segment}"))
                        .uri(downstream.getIdentity()))
                .route("project-v2", route -> route
                        .path("/api/projects/**")
                        .and().header("X-API-Version", "2")
                        .filters(filter -> filter.rewritePath("/api/projects(?<segment>/?.*)", "/v2/projects${segment}"))
                        .uri(downstream.getProject()))
                .route("project-v1", route -> route
                        .path("/api/projects/**")
                        .filters(filter -> filter.rewritePath("/api/projects(?<segment>/?.*)", "/v1/projects${segment}"))
                        .uri(downstream.getProject()))
                .route("planning-v2", route -> route
                        .path("/api/planning/**")
                        .and().header("X-API-Version", "2")
                        .filters(filter -> filter.rewritePath("/api/planning(?<segment>/?.*)", "/v2${segment}"))
                        .uri(downstream.getPlanning()))
                .route("planning-v1", route -> route
                        .path("/api/planning/**")
                        .filters(filter -> filter.rewritePath("/api/planning(?<segment>/?.*)", "/v1${segment}"))
                        .uri(downstream.getPlanning()))
                .route("calendar", route -> route
                        .path("/api/calendar/**")
                        .filters(filter -> filter
                                .circuitBreaker(config -> config.setName("calendarService").setFallbackUri("forward:/fallback/calendar"))
                                .rewritePath("/api/calendar(?<segment>/?.*)", "/v1/calendar${segment}"))
                        .uri(downstream.getCalendar()))
                .route("meeting", route -> route
                        .path("/api/meetings/**")
                        .filters(filter -> filter
                                .circuitBreaker(config -> config.setName("meetingService").setFallbackUri("forward:/fallback/meeting"))
                                .rewritePath("/api/meetings(?<segment>/?.*)", "/v1/meetings${segment}"))
                        .uri(downstream.getMeeting()))
                .route("sync", route -> route
                        .path("/api/sync/**")
                        .filters(filter -> filter
                                .circuitBreaker(config -> config.setName("syncService").setFallbackUri("forward:/fallback/sync"))
                                .rewritePath("/api/sync(?<segment>/?.*)", "/v1/sync${segment}"))
                        .uri(downstream.getSync()))
                .route("resource", route -> route
                        .path("/api/resources/**")
                        .filters(filter -> filter
                                .circuitBreaker(config -> config.setName("resourceService").setFallbackUri("forward:/fallback/resource"))
                                .rewritePath("/api/resources(?<segment>/?.*)", "/v1${segment}"))
                        .uri(downstream.getResource()))
                .build();
    }
}
