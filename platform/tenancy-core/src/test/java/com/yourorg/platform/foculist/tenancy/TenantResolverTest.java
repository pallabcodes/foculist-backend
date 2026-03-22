package com.yourorg.platform.foculist.tenancy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import static org.mockito.Mockito.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantResolverTest {
    private static final String JWT_SECRET = "Zm9jdWxpc3QtZGV2LWp3dC1zZWNyZXQta2VlcC1jaGFuZ2U=";

    @Test
    void resolvesFromHeader() {
        TenantContextProperties properties = new TenantContextProperties();
        RestTemplate restTemplate = mock(RestTemplate.class);
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET, "http://mock-keycloak", restTemplate));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "tenant-a");

        assertThat(resolver.resolve(null, "tenant-a", null, null, null)).isEqualTo("tenant-a");
    }

    @Test
    void rejectsWhenTenantMissingAndRequired() {
        TenantContextProperties properties = new TenantContextProperties();
        RestTemplate restTemplate = mock(RestTemplate.class);
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET, "http://mock-keycloak", restTemplate));

        assertThatThrownBy(() -> resolver.resolve(null, null, null, null, null))
                .isInstanceOf(TenantResolutionException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void resolvesFromPathWhenHeaderMissing() {
        TenantContextProperties properties = new TenantContextProperties();
        RestTemplate restTemplate = mock(RestTemplate.class);
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET, "http://mock-keycloak", restTemplate));

        assertThat(resolver.resolve(null, null, null, "/v1/tenants/acme/projects", null)).isEqualTo("acme");
    }

    @Test
    void rejectsInvalidJwtWhenAuthorizationHeaderIsPresent() {
        TenantContextProperties properties = new TenantContextProperties();
        RestTemplate restTemplate = mock(RestTemplate.class);
        TenantResolver resolver = new TenantResolver(properties, new JwtClaimExtractor(JWT_SECRET, "http://mock-keycloak", restTemplate));

        assertThatThrownBy(() -> resolver.resolve("Bearer invalid-token", "tenant-a", null, "/v1/projects", null))
                .isInstanceOf(TenantResolutionException.class)
                .satisfies(ex -> assertThat(((TenantResolutionException) ex).getStatus()).isEqualTo(401));
    }
}
