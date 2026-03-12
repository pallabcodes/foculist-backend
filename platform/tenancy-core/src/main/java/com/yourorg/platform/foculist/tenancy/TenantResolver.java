package com.yourorg.platform.foculist.tenancy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public class TenantResolver {
    private final TenantContextProperties properties;
    private final JwtClaimExtractor jwtClaimExtractor;

    public TenantResolver(TenantContextProperties properties, JwtClaimExtractor jwtClaimExtractor) {
        this.properties = properties;
        this.jwtClaimExtractor = jwtClaimExtractor;
    }

    /**
     * Environment-agnostic resolution logic.
     */
    public String resolve(String authHeader, String tenantHeader, String tenantParam, String requestUri, String serverName) {
        String jwtTenant = null;
        if (properties.isAllowJwtClaim()) {
            try {
                jwtTenant = jwtClaimExtractor.extractClaim(authHeader, properties.getJwtClaim());
            } catch (JwtValidationException ex) {
                throw new TenantResolutionException(401, ex.getMessage());
            }
        }

        String tenant = firstNonBlank(
                tenantHeader,
                tenantParam,
                extractFromPath(requestUri),
                extractFromSubdomain(serverName),
                jwtTenant
        );

        if (!StringUtils.hasText(tenant)) {
            if (properties.isRequired()) {
                throw new TenantResolutionException(400, "Tenant identifier is missing");
            }
            tenant = properties.getDefaultTenant();
        }

        if (StringUtils.hasText(jwtTenant)
                && StringUtils.hasText(tenant)
                && properties.isEnforceJwtMatch()
                && !tenant.equals(jwtTenant)) {
            throw new TenantResolutionException(403, "Tenant identifier does not match JWT tenant claim");
        }

        return tenant;
    }

    private String extractFromPath(String requestUri) {
        if (requestUri == null) return null;
        Pattern pattern = Pattern.compile(properties.getPathPattern());
        Matcher matcher = pattern.matcher(requestUri);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String extractFromSubdomain(String serverName) {
        if (!StringUtils.hasText(serverName)) {
            return null;
        }
        Pattern pattern = Pattern.compile(properties.getSubdomainPattern());
        Matcher matcher = pattern.matcher(serverName);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
