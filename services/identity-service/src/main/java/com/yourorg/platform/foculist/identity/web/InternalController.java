package com.yourorg.platform.foculist.identity.web;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OrganizationRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/internal")
@RequiredArgsConstructor
@PreAuthorize("@orgSecurity.isPlatformAdmin()")
public class InternalController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @GetMapping("/users")
    public List<InternalUserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(u -> new InternalUserResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getGlobalRole() != null ? u.getGlobalRole().name() : "USER",
                        u.isActive()
                ))
                .toList();
    }

    @GetMapping("/orgs")
    public List<InternalOrgResponse> getOrgs() {
        return organizationRepository.findAll().stream()
                .map(o -> new InternalOrgResponse(
                        o.getId(),
                        o.getName(),
                        o.getSlug(),
                        o.getMetadata()
                ))
                .toList();
    }

    @PostMapping("/users/{id}/suspend")
    @ResponseStatus(HttpStatus.OK)
    public void suspendUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @PostMapping("/users/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public void activateUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(true);
        userRepository.save(user);
    }

    public record InternalUserResponse(UUID id, String name, String email, String globalRole, boolean isActive) {}
    public record InternalOrgResponse(UUID id, String name, String slug, java.util.Map<String, Object> metadata) {}
}
