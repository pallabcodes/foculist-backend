package com.yourorg.platform.foculist.identity.clean.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OrganizationRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private UUID userId;
    private UUID orgId;
    private User mockUser;
    private Organization mockOrg;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("test@example.com");
        mockUser.setTenantId("public");

        mockOrg = new Organization();
        mockOrg.setId(orgId);
        mockOrg.setTenantId("test-tenant");
        mockOrg.setName("Test Org");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createOrganization_success() {
        when(userRepository.findByIdAndTenantId(userId, "test-tenant")).thenReturn(Optional.of(mockUser));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));

        Organization result = organizationService.createOrganization("Test Org", "test-slug", userId);

        assertNotNull(result);
        assertEquals("Test Org", result.getName());
        assertEquals("test-slug", result.getTenantId());
        
        verify(membershipRepository).save(any(Membership.class));
    }

    @Test
    void addMember_success() {
        when(organizationRepository.findByIdAndTenantId(orgId, "test-tenant")).thenReturn(Optional.of(mockOrg));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(mockUser));
        when(membershipRepository.findByOrganizationAndUserAndTenantId(mockOrg, mockUser, "test-tenant"))
                .thenReturn(Optional.empty());

        organizationService.addMember(orgId, "new@example.com", MembershipRole.MEMBER);

        verify(membershipRepository).save(any(Membership.class));
    }

    @Test
    void addMember_failsWhenOrgNotInContext() {
        // Org not found in the current tenant isolation context
        when(organizationRepository.findByIdAndTenantId(orgId, "test-tenant")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            organizationService.addMember(orgId, "new@example.com", MembershipRole.MEMBER);
        });
        
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Organization not found"));
    }
}
