package com.yourorg.platform.foculist.identity.web;

import com.yourorg.platform.foculist.identity.clean.application.service.InviteService;
import com.yourorg.platform.foculist.identity.clean.application.service.OrganizationService;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.yourorg.platform.foculist.identity.clean.application.service.OnboardingService;

@RestController
@RequestMapping("/v1/onboard")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardResponse onboard(
            @Valid @RequestBody OnboardRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = requireUserId(jwt);

        Organization org = onboardingService.onboard(
                request.projectName(),
                request.projectKey(),
                userId,
                request.experience(),
                request.invites()
        );

        return new OnboardResponse(org.getId(), org.getSlug(), org.getName(), org.getMetadata());
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

    public record OnboardRequest(
            @NotBlank String projectName,
            @NotBlank String projectKey,
            @NotBlank String experience,
            List<String> invites
    ) {}

    public record OnboardResponse(
            UUID id,
            String slug,
            String name,
            Map<String, Object> metadata
    ) {}
}
