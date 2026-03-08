package com.yourorg.platform.foculist.gateway.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class GatewayCorsProperties {
    private String allowedOrigins = "http://localhost:3000";
    private String allowedMethods = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private String allowedHeaders = "*";
    private boolean allowCredentials = true;

    public List<String> allowedOrigins() {
        return split(allowedOrigins);
    }

    public List<String> allowedMethods() {
        return split(allowedMethods);
    }

    public List<String> allowedHeaders() {
        return split(allowedHeaders);
    }

    private List<String> split(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
