package com.yourorg.platform.foculist.resource.clean.application.service;

import com.yourorg.platform.foculist.resource.clean.application.command.CreateBookmarkCommand;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateVaultItemCommand;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateWorklogCommand;
import com.yourorg.platform.foculist.resource.clean.application.view.BookmarkView;
import com.yourorg.platform.foculist.resource.clean.application.view.VaultItemView;
import com.yourorg.platform.foculist.resource.clean.application.view.WorklogEntryView;
import com.yourorg.platform.foculist.resource.clean.domain.model.Bookmark;
import com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException;
import com.yourorg.platform.foculist.resource.clean.domain.model.VaultItem;
import com.yourorg.platform.foculist.resource.clean.domain.model.WorklogEntry;
import com.yourorg.platform.foculist.resource.clean.domain.port.BookmarkRepositoryPort;
import com.yourorg.platform.foculist.resource.clean.domain.port.VaultItemRepositoryPort;
import com.yourorg.platform.foculist.resource.clean.domain.port.WorklogEntryRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceApplicationServiceTest {

    @Test
    void createsAndListsBookmarksForTenant() {
        Instant now = Instant.parse("2026-02-02T10:15:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        BookmarkRepositoryPort bookmarkRepository = mock(BookmarkRepositoryPort.class);
        WorklogEntryRepositoryPort worklogEntryRepository = mock(WorklogEntryRepositoryPort.class);
        VaultItemRepositoryPort vaultItemRepository = mock(VaultItemRepositoryPort.class);
        ResourceApplicationService service = new ResourceApplicationService(
                bookmarkRepository,
                worklogEntryRepository,
                vaultItemRepository,
                clock
        );

        when(bookmarkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BookmarkView created = service.createBookmark(
                "tenant-a",
                new CreateBookmarkCommand("Design Docs", "https://example.com/design")
        );

        assertThat(created.tenantId()).isEqualTo("tenant-a");
        assertThat(created.title()).isEqualTo("Design Docs");
        assertThat(created.url()).isEqualTo("https://example.com/design");

        Bookmark existing = Bookmark.create("tenant-a", "Runbook", "https://example.com/runbook", now.minusSeconds(30));
        when(bookmarkRepository.findByTenantId("tenant-a", 0, 50)).thenReturn(List.of(createdToDomain(created), existing));

        List<BookmarkView> bookmarks = service.listBookmarks("tenant-a", 0, 50);
        assertThat(bookmarks).hasSize(2);
        assertThat(bookmarks.get(0).tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void rejectsInvalidBookmarkUrl() {
        BookmarkRepositoryPort bookmarkRepository = mock(BookmarkRepositoryPort.class);
        WorklogEntryRepositoryPort worklogEntryRepository = mock(WorklogEntryRepositoryPort.class);
        VaultItemRepositoryPort vaultItemRepository = mock(VaultItemRepositoryPort.class);
        ResourceApplicationService service = new ResourceApplicationService(
                bookmarkRepository,
                worklogEntryRepository,
                vaultItemRepository
        );

        assertThatThrownBy(() -> service.createBookmark(
                "tenant-a",
                new CreateBookmarkCommand("Bad", "ftp://example.com/file")
        )).isInstanceOf(ResourceDomainException.class).hasMessageContaining("http or https");
    }

    @Test
    void rejectsInvalidWorklogDuration() {
        BookmarkRepositoryPort bookmarkRepository = mock(BookmarkRepositoryPort.class);
        WorklogEntryRepositoryPort worklogEntryRepository = mock(WorklogEntryRepositoryPort.class);
        VaultItemRepositoryPort vaultItemRepository = mock(VaultItemRepositoryPort.class);
        ResourceApplicationService service = new ResourceApplicationService(
                bookmarkRepository,
                worklogEntryRepository,
                vaultItemRepository
        );

        assertThatThrownBy(() -> service.createWorklog(
                "tenant-a",
                new CreateWorklogCommand("Project A", "Task A", 0)
        )).isInstanceOf(ResourceDomainException.class).hasMessageContaining("durationMinutes");
    }

    @Test
    void createsVaultItemWithDefaultClassification() {
        BookmarkRepositoryPort bookmarkRepository = mock(BookmarkRepositoryPort.class);
        WorklogEntryRepositoryPort worklogEntryRepository = mock(WorklogEntryRepositoryPort.class);
        VaultItemRepositoryPort vaultItemRepository = mock(VaultItemRepositoryPort.class);
        ResourceApplicationService service = new ResourceApplicationService(
                bookmarkRepository,
                worklogEntryRepository,
                vaultItemRepository
        );
        when(vaultItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VaultItemView created = service.createVaultItem(
                "tenant-a",
                new CreateVaultItemCommand("Incident Notes", null)
        );
        assertThat(created.classification()).isEqualTo("INTERNAL");
    }

    @Test
    void createsWorklogEntry() {
        BookmarkRepositoryPort bookmarkRepository = mock(BookmarkRepositoryPort.class);
        WorklogEntryRepositoryPort worklogEntryRepository = mock(WorklogEntryRepositoryPort.class);
        VaultItemRepositoryPort vaultItemRepository = mock(VaultItemRepositoryPort.class);
        ResourceApplicationService service = new ResourceApplicationService(
                bookmarkRepository,
                worklogEntryRepository,
                vaultItemRepository
        );
        when(worklogEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorklogEntryView created = service.createWorklog(
                "tenant-a",
                new CreateWorklogCommand("Platform", "Tune query", 45)
        );

        assertThat(created.durationMinutes()).isEqualTo(45);
        assertThat(created.project()).isEqualTo("Platform");
    }

    private Bookmark createdToDomain(BookmarkView view) {
        return new Bookmark(
                view.id(),
                view.tenantId(),
                view.title(),
                view.url(),
                view.createdAt(),
                0L
        );
    }
}
