package com.yourorg.platform.foculist.tenancy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantResolverTest {
    private static final String JWT_SECRET = "Zm9jdWxpc3QtZGV2LWp3dC1zZWNyZXQta2VlcC1jaGFuZ2U=";

    @Test
    void resolvesFromHeader() {
        TenantContextProperties properties = new TenantContextProperties();
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "tenant-a");

        assertThat(resolver.resolve(request)).isEqualTo("tenant-a");
    }

    @Test
    void rejectsWhenTenantMissingAndRequired() {
        TenantContextProperties properties = new TenantContextProperties();
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET));

        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(TenantResolutionException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void resolvesFromPathWhenHeaderMissing() {
        TenantContextProperties properties = new TenantContextProperties();
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/tenants/acme/projects");

        assertThat(resolver.resolve(request)).isEqualTo("acme");
    }

    @Test
    void rejectsInvalidJwtWhenAuthorizationHeaderIsPresent() {
        TenantContextProperties properties = new TenantContextProperties();
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/projects");
        request.addHeader("X-Tenant-ID", "tenant-a");
        request.addHeader("Authorization", "Bearer invalid-token");

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(TenantResolutionException.class)
                .satisfies(ex -> assertThat(((TenantResolutionException) ex).getStatus()).isEqualTo(401));
    }
}
