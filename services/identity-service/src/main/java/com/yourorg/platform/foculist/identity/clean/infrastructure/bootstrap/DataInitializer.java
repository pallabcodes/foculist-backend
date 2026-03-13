package com.yourorg.platform.foculist.identity.clean.infrastructure.bootstrap;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OrganizationRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            log.info("Database already initialized. Skipping bootstrap seeding.");
            return;
        }

        log.info("🚀 Initializing Sovereign Data Scaffolding (Google-Grade Scaling)...");

        // 1. Create System Organization
        Organization systemOrg = new Organization();
        systemOrg.setName("Foculist Global");
        systemOrg.setSlug("foculist");
        systemOrg.setTenantId("public");
        systemOrg.setTier("ENTERPRISE");
        systemOrg.setCreatedBy("system-bootstrap");
        systemOrg.setMetadata(Map.of(
            "features", Map.of("sso_enabled", true, "custom_domains", true),
            "region", "global-system"
        ));
        organizationRepository.save(systemOrg);

        // 2. Create Root Admin
        User rootAdmin = new User();
        rootAdmin.setEmail("admin@foculist.com");
        rootAdmin.setFullName("Platform Administrator");
        rootAdmin.setPasswordHash(passwordEncoder.encode("admin123")); // Default L5 bootstrap password
        rootAdmin.setTenantId("public");
        rootAdmin.setGlobalRole("ADMIN");
        rootAdmin.setCreatedBy("system-bootstrap");
        rootAdmin.setMetadata(Map.of("onboarded", true, "security_level", "LEVEL_5"));
        userRepository.save(rootAdmin);

        // 3. Link Admin to System Org
        Membership systemMembership = new Membership();
        systemMembership.setOrganization(systemOrg);
        systemMembership.setUser(rootAdmin);
        systemMembership.setTenantId("public");
        systemMembership.setRole(MembershipRole.OWNER);
        systemMembership.setStatus("ACTIVE");
        systemMembership.setCreatedBy("system-bootstrap");
        membershipRepository.save(systemMembership);

        // 4. Create Demo Team Tenant
        Organization demoOrg = new Organization();
        demoOrg.setName("Project Apollo (Demo)");
        demoOrg.setSlug("apollo-team");
        demoOrg.setTenantId("apollo-team");
        demoOrg.setTier("PRO");
        demoOrg.setCreatedBy("system-bootstrap");
        demoOrg.setMetadata(Map.of("context", "demonstration-unit"));
        organizationRepository.save(demoOrg);

        // 5. Create Test User for Demo Team
        User testUser = new User();
        testUser.setEmail("lead@apollo.team");
        testUser.setFullName("Mars Rover");
        testUser.setPasswordHash(passwordEncoder.encode("apollo123"));
        testUser.setTenantId("apollo-team");
        testUser.setGlobalRole("USER");
        testUser.setCreatedBy("system-bootstrap");
        userRepository.save(testUser);

        // 6. Link Test User
        Membership demoMembership = new Membership();
        demoMembership.setOrganization(demoOrg);
        demoMembership.setUser(testUser);
        demoMembership.setTenantId("apollo-team");
        demoMembership.setRole(MembershipRole.ADMIN);
        demoMembership.setCreatedBy("system-bootstrap");
        membershipRepository.save(demoMembership);

        log.info("✅ Database successfully seeded with Sovereign Command data.");
    }
}
