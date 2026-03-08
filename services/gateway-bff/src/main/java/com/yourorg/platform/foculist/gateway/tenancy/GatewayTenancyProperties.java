package com.yourorg.platform.foculist.gateway.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tenancy")
public class GatewayTenancyProperties {
    private String defaultTenant = "public";
    private String header = "X-Tenant-ID";
    private String parameter = "tenant";
    private String pathPattern = "^/api/tenants/([^/]+)/.*";
    private String subdomainPattern = "^([^.]+)\\..*";
    private boolean required = true;
    private boolean allowJwtClaim = true;
    private String jwtClaim = "tenant";
    private boolean enforceJwtMatch = true;
    private boolean skipActuator = true;

    public String getDefaultTenant() {
        return defaultTenant;
    }

    public void setDefaultTenant(String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getSubdomainPattern() {
        return subdomainPattern;
    }

    public void setSubdomainPattern(String subdomainPattern) {
        this.subdomainPattern = subdomainPattern;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isAllowJwtClaim() {
        return allowJwtClaim;
    }

    public void setAllowJwtClaim(boolean allowJwtClaim) {
        this.allowJwtClaim = allowJwtClaim;
    }

    public String getJwtClaim() {
        return jwtClaim;
    }

    public void setJwtClaim(String jwtClaim) {
        this.jwtClaim = jwtClaim;
    }

    public boolean isEnforceJwtMatch() {
        return enforceJwtMatch;
    }

    public void setEnforceJwtMatch(boolean enforceJwtMatch) {
        this.enforceJwtMatch = enforceJwtMatch;
    }

    public boolean isSkipActuator() {
        return skipActuator;
    }

    public void setSkipActuator(boolean skipActuator) {
        this.skipActuator = skipActuator;
    }
}
