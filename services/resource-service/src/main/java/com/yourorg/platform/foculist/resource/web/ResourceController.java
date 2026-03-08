package com.yourorg.platform.foculist.resource.web;

import com.yourorg.platform.foculist.resource.clean.application.command.CreateBookmarkCommand;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateVaultItemCommand;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateWorklogCommand;
import com.yourorg.platform.foculist.resource.clean.application.service.ResourceApplicationService;
import com.yourorg.platform.foculist.resource.clean.application.view.BookmarkView;
import com.yourorg.platform.foculist.resource.clean.application.view.VaultItemView;
import com.yourorg.platform.foculist.resource.clean.application.view.WorklogEntryView;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Validated
public class ResourceController {
    private final ResourceApplicationService resourceApplicationService;

    public ResourceController(ResourceApplicationService resourceApplicationService) {
        this.resourceApplicationService = resourceApplicationService;
    }

    @GetMapping("/bookmarks")
    public List<BookmarkView> listBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return resourceApplicationService.listBookmarks(TenantContext.require(), boundedPage, boundedSize);
    }

    @PostMapping("/bookmarks")
    public ResponseEntity<BookmarkView> createBookmark(@Valid @RequestBody CreateBookmarkRequest request) {
        BookmarkView created = resourceApplicationService.createBookmark(
                TenantContext.require(),
                new CreateBookmarkCommand(request.title(), request.url())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/worklog/entries")
    public ResponseEntity<WorklogEntryView> createWorklog(@Valid @RequestBody CreateWorklogRequest request) {
        WorklogEntryView created = resourceApplicationService.createWorklog(
                TenantContext.require(),
                new CreateWorklogCommand(request.project(), request.task(), request.durationMinutes())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/vault/items")
    public ResponseEntity<VaultItemView> createVaultItem(@Valid @RequestBody CreateVaultItemRequest request) {
        VaultItemView created = resourceApplicationService.createVaultItem(
                TenantContext.require(),
                new CreateVaultItemCommand(request.name(), request.classification())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/bookmarks/{bookmarkId}")
    public ResponseEntity<BookmarkView> updateBookmark(@PathVariable UUID bookmarkId, @Valid @RequestBody UpdateBookmarkRequest request) {
        BookmarkView updated = resourceApplicationService.updateBookmark(
                TenantContext.require(),
                bookmarkId,
                new com.yourorg.platform.foculist.resource.clean.application.command.UpdateBookmarkCommand(request.title(), request.url())
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/bookmarks/{bookmarkId}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable UUID bookmarkId) {
        resourceApplicationService.deleteBookmark(TenantContext.require(), bookmarkId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/worklog/entries")
    public List<WorklogEntryView> listWorklogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return resourceApplicationService.listWorklogs(TenantContext.require(), boundedPage, boundedSize);
    }

    @PutMapping("/worklog/entries/{worklogId}")
    public ResponseEntity<WorklogEntryView> updateWorklog(@PathVariable UUID worklogId, @Valid @RequestBody UpdateWorklogRequest request) {
        WorklogEntryView updated = resourceApplicationService.updateWorklog(
                TenantContext.require(),
                worklogId,
                new com.yourorg.platform.foculist.resource.clean.application.command.UpdateWorklogCommand(request.project(), request.task(), request.durationMinutes())
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/worklog/entries/{worklogId}")
    public ResponseEntity<Void> deleteWorklog(@PathVariable UUID worklogId) {
        resourceApplicationService.deleteWorklog(TenantContext.require(), worklogId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vault/items")
    public List<VaultItemView> listVaultItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        int boundedPage = Math.max(page, 0);
        return resourceApplicationService.listVaultItems(TenantContext.require(), boundedPage, boundedSize);
    }

    @PutMapping("/vault/items/{vaultItemId}")
    public ResponseEntity<VaultItemView> updateVaultItem(@PathVariable UUID vaultItemId, @Valid @RequestBody UpdateVaultItemRequest request) {
        VaultItemView updated = resourceApplicationService.updateVaultItem(
                TenantContext.require(),
                vaultItemId,
                new com.yourorg.platform.foculist.resource.clean.application.command.UpdateVaultItemCommand(request.name(), request.classification())
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/vault/items/{vaultItemId}")
    public ResponseEntity<Void> deleteVaultItem(@PathVariable UUID vaultItemId) {
        resourceApplicationService.deleteVaultItem(TenantContext.require(), vaultItemId);
        return ResponseEntity.noContent().build();
    }

    public record CreateBookmarkRequest(
            @NotBlank String title,
            @NotBlank String url
    ) {
    }

    public record CreateWorklogRequest(
            @NotBlank String project,
            @NotBlank String task,
            int durationMinutes
    ) {
    }

    public record CreateVaultItemRequest(
            @NotBlank String name,
            String classification
    ) {
    }

    public record UpdateBookmarkRequest(
            @NotBlank String title,
            @NotBlank String url
    ) {
    }

    public record UpdateWorklogRequest(
            @NotBlank String project,
            @NotBlank String task,
            int durationMinutes
    ) {
    }

    public record UpdateVaultItemRequest(
            @NotBlank String name,
            String classification
    ) {
    }
}
