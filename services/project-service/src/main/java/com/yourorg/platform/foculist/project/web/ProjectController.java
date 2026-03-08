package com.yourorg.platform.foculist.project.web;

import com.yourorg.platform.foculist.project.application.CreateProjectCommand;
import com.yourorg.platform.foculist.project.application.ProjectApplicationService;
import com.yourorg.platform.foculist.project.application.ProjectSettingsView;
import com.yourorg.platform.foculist.project.application.ProjectSummaryView;
import com.yourorg.platform.foculist.project.application.UpdateProjectSettingsCommand;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/projects")
@Validated
public class ProjectController {
    private final ProjectApplicationService projectApplicationService;

    public ProjectController(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = projectApplicationService;
    }

    @GetMapping
    public List<ProjectSummaryView> listProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return projectApplicationService.listProjects(TenantContext.require(), boundedPage, boundedSize);
    }

    @PostMapping
    public ResponseEntity<ProjectSummaryView> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectSummaryView created = projectApplicationService.createProject(
                TenantContext.require(),
                new CreateProjectCommand(
                        request.name(),
                        request.description(),
                        request.status(),
                        request.priority(),
                        request.dueDate()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/settings")
    public ProjectSettingsView updateSettings(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectSettingsRequest request
    ) {
        return projectApplicationService.updateSettings(
                TenantContext.require(),
                id,
                new UpdateProjectSettingsCommand(request.workflowStatuses(), request.defaultView())
        );
    }

    public record CreateProjectRequest(
            @NotBlank String name,
            String description,
            String status,
            String priority,
            String dueDate
    ) {
    }

    public record ProjectSettingsRequest(
            List<String> workflowStatuses,
            String defaultView
    ) {
    }
}
