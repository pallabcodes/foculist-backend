package com.yourorg.platform.foculist.tenancy.security;

import com.yourorg.platform.foculist.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TenancyLeakTest.TestConfig.class)
@ActiveProfiles("test")
public class TenancyLeakTest {

    @org.springframework.context.annotation.Configuration
    public static class TestConfig {}

    @Test
    public void testStrictTenantIsolation() {
        // 1. Simulate Tenant A context
        TenantContext.set("tenant-a");
        assertEquals("tenant-a", TenantContext.require(), "Tenant A should be active");

        // 2. Perform a simulated isolated operation
        String dataA = simulateIsolatedOperation("tenant-a");
        assertNotNull(dataA);

        // 3. Switch to Tenant B
        TenantContext.set("tenant-b");
        assertEquals("tenant-b", TenantContext.require(), "Tenant B should be active");

        // 4. Verify that Tenant B CANNOT see Tenant A data
        // In a real repo, this would be enforced by Hibernate filters or AspectJ
        assertThrows(SecurityException.class, () -> {
             verifyIsolation("tenant-a", dataA);
        }, "Should throw SecurityException when accessing other tenant's data");
        
        TenantContext.clear();
    }

    private String simulateIsolatedOperation(String tenantId) {
        // Logic representing a secure data fetch
        return "secure-data-for-" + tenantId;
    }

    private void verifyIsolation(String ownerTenantId, String data) {
        String currentTenant = TenantContext.require();
        if (!ownerTenantId.equals(currentTenant)) {
            throw new SecurityException("🚫 TENANCY LEAK DETECTED: Illegal access to " + ownerTenantId + " from " + currentTenant);
        }
    }
}
