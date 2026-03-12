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
                        .filters(filter -> filter.rewritePath("/api/calendar(?<segment>/?.*)", "/v1/calendar${segment}"))
                        .uri(downstream.getCalendar()))
                .route("meeting", route -> route
                        .path("/api/meetings/**")
                        .filters(filter -> filter.rewritePath("/api/meetings(?<segment>/?.*)", "/v1/meetings${segment}"))
                        .uri(downstream.getMeeting()))
                .route("sync", route -> route
                        .path("/api/sync/**")
                        .filters(filter -> filter.rewritePath("/api/sync(?<segment>/?.*)", "/v1/sync${segment}"))
                        .uri(downstream.getSync()))
                .route("resource", route -> route
                        .path("/api/resources/**")
                        .filters(filter -> filter.rewritePath("/api/resources(?<segment>/?.*)", "/v1${segment}"))
                        .uri(downstream.getResource()))
                // Internal OpenAPI routes for aggregation
                .route("identity-docs", route -> route
                        .path("/api/identity/v3/api-docs")
                        .filters(f -> f.rewritePath("/api/identity/v3/api-docs", "/v3/api-docs"))
                        .uri(downstream.getIdentity()))
                .route("planning-docs", route -> route
                        .path("/api/planning/v3/api-docs")
                        .filters(f -> f.rewritePath("/api/planning/v3/api-docs", "/v3/api-docs"))
                        .uri(downstream.getPlanning()))
                .route("project-docs", route -> route
                        .path("/api/project/v3/api-docs")
                        .filters(f -> f.rewritePath("/api/project/v3/api-docs", "/v3/api-docs"))
                        .uri(downstream.getProject()))
                .route("sync-docs", route -> route
                        .path("/api/sync/v3/api-docs")
                        .filters(f -> f.rewritePath("/api/sync/v3/api-docs", "/v3/api-docs"))
                        .uri(downstream.getSync()))
                .build();
    }
}
