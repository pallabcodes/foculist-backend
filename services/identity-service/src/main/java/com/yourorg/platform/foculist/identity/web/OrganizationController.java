package com.yourorg.platform.foculist.identity.web;

import com.yourorg.platform.foculist.identity.clean.application.service.OrganizationService;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/orgs")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organization createOrganization(
            @Valid @RequestBody CreateOrgRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return organizationService.createOrganization(request.name(), request.slug(), requireUserId(jwt));
    }

    @GetMapping
    public List<Organization> getOrganizations(@AuthenticationPrincipal Jwt jwt) {
        return organizationService.getUserOrganizations(requireUserId(jwt));
    }

    @PostMapping("/{orgId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@PathVariable UUID orgId, @Valid @RequestBody AddMemberRequest request) {
        organizationService.addMember(orgId, request.email(), request.role());
    }

    @GetMapping("/{orgId}/members")
    public List<MemberResponse> getMembers(@PathVariable UUID orgId) {
        return organizationService.getMembers(orgId).stream()
                .map(m -> new MemberResponse(
                        m.getUser().getId().toString(),
                        m.getUser().getFullName() != null ? m.getUser().getFullName() : m.getUser().getEmail(),
                        m.getUser().getEmail(),
                        m.getRole().name()
                ))
                .toList();
    }

    private UUID requireUserId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        String rawUserId = jwt.getClaimAsString("userId");
        if (!StringUtils.hasText(rawUserId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT claim userId is missing");
        }
        try {
            return UUID.fromString(rawUserId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT claim userId is invalid");
        }
    }

    public record CreateOrgRequest(@NotBlank String name, @NotBlank String slug) {}
    public record AddMemberRequest(@Email String email, MembershipRole role) {}
    public record MemberResponse(String id, String name, String email, String role) {}
}
