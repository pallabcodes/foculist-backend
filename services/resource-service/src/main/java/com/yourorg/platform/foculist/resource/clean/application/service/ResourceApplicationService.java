package com.yourorg.platform.foculist.resource.clean.application.service;

import com.yourorg.platform.foculist.resource.clean.application.command.CreateBookmarkCommand;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateVaultItemCommand;
import com.yourorg.platform.foculist.resource.clean.application.command.CreateWorklogCommand;
import com.yourorg.platform.foculist.resource.clean.application.view.BookmarkView;
import com.yourorg.platform.foculist.resource.clean.application.view.VaultItemView;
import com.yourorg.platform.foculist.resource.clean.application.view.WorklogEntryView;
import com.yourorg.platform.foculist.resource.clean.domain.model.Bookmark;
import com.yourorg.platform.foculist.resource.clean.domain.model.VaultItem;
import com.yourorg.platform.foculist.resource.clean.domain.model.WorklogEntry;
import com.yourorg.platform.foculist.resource.clean.domain.port.BookmarkRepositoryPort;
import com.yourorg.platform.foculist.resource.clean.domain.port.VaultItemRepositoryPort;
import com.yourorg.platform.foculist.resource.clean.domain.port.WorklogEntryRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceApplicationService {
    private final BookmarkRepositoryPort bookmarkRepository;
    private final WorklogEntryRepositoryPort worklogEntryRepository;
    private final VaultItemRepositoryPort vaultItemRepository;
    private final Clock clock;

    public ResourceApplicationService(
            BookmarkRepositoryPort bookmarkRepository,
            WorklogEntryRepositoryPort worklogEntryRepository,
            VaultItemRepositoryPort vaultItemRepository,
            Clock clock
    ) {
        this.bookmarkRepository = bookmarkRepository;
        this.worklogEntryRepository = worklogEntryRepository;
        this.vaultItemRepository = vaultItemRepository;
        this.clock = clock;
    }

    public ResourceApplicationService(
            BookmarkRepositoryPort bookmarkRepository,
            WorklogEntryRepositoryPort worklogEntryRepository,
            VaultItemRepositoryPort vaultItemRepository
    ) {
        this(bookmarkRepository, worklogEntryRepository, vaultItemRepository, Clock.systemUTC());
    }

    @Transactional(readOnly = true)
    public List<BookmarkView> listBookmarks(String tenantId, int page, int size) {
        return bookmarkRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toBookmarkView)
                .toList();
    }

    @Transactional
    public BookmarkView createBookmark(String tenantId, CreateBookmarkCommand command) {
        Bookmark created = bookmarkRepository.save(Bookmark.create(
                tenantId,
                command.title(),
                command.url(),
                Instant.now(clock)
        ));
        return toBookmarkView(created);
    }

    @Transactional
    public WorklogEntryView createWorklog(String tenantId, CreateWorklogCommand command) {
        WorklogEntry created = worklogEntryRepository.save(WorklogEntry.create(
                tenantId,
                command.project(),
                command.task(),
                command.durationMinutes(),
                Instant.now(clock)
        ));
        return toWorklogView(created);
    }

    @Transactional
    public VaultItemView createVaultItem(String tenantId, CreateVaultItemCommand command) {
        VaultItem created = vaultItemRepository.save(VaultItem.create(
                tenantId,
                command.name(),
                command.classification(),
                Instant.now(clock)
        ));
        return toVaultItemView(created);
    }

    @Transactional
    public BookmarkView updateBookmark(String tenantId, java.util.UUID bookmarkId, com.yourorg.platform.foculist.resource.clean.application.command.UpdateBookmarkCommand command) {
        Bookmark bookmark = bookmarkRepository.findByIdAndTenantId(bookmarkId, tenantId)
                .orElseThrow(() -> new com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException("Bookmark not found"));
        Bookmark updated = bookmark.update(command.title(), command.url());
        return toBookmarkView(bookmarkRepository.save(updated));
    }

    @Transactional
    public void deleteBookmark(String tenantId, java.util.UUID bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findByIdAndTenantId(bookmarkId, tenantId)
                .orElseThrow(() -> new com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException("Bookmark not found"));
        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public List<WorklogEntryView> listWorklogs(String tenantId, int page, int size) {
        return worklogEntryRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toWorklogView)
                .toList();
    }

    @Transactional
    public WorklogEntryView updateWorklog(String tenantId, java.util.UUID worklogId, com.yourorg.platform.foculist.resource.clean.application.command.UpdateWorklogCommand command) {
        WorklogEntry worklog = worklogEntryRepository.findByIdAndTenantId(worklogId, tenantId)
                .orElseThrow(() -> new com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException("Worklog not found"));
        WorklogEntry updated = worklog.update(command.project(), command.task(), command.durationMinutes());
        return toWorklogView(worklogEntryRepository.save(updated));
    }

    @Transactional
    public void deleteWorklog(String tenantId, java.util.UUID worklogId) {
        WorklogEntry worklog = worklogEntryRepository.findByIdAndTenantId(worklogId, tenantId)
                .orElseThrow(() -> new com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException("Worklog not found"));
        worklogEntryRepository.delete(worklog);
    }

    @Transactional(readOnly = true)
    public List<VaultItemView> listVaultItems(String tenantId, int page, int size) {
        return vaultItemRepository.findByTenantId(tenantId, page, size).stream()
                .map(this::toVaultItemView)
                .toList();
    }

    @Transactional
    public VaultItemView updateVaultItem(String tenantId, java.util.UUID vaultItemId, com.yourorg.platform.foculist.resource.clean.application.command.UpdateVaultItemCommand command) {
        VaultItem vaultItem = vaultItemRepository.findByIdAndTenantId(vaultItemId, tenantId)
                .orElseThrow(() -> new com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException("Vault item not found"));
        VaultItem updated = vaultItem.update(command.name(), command.classification());
        return toVaultItemView(vaultItemRepository.save(updated));
    }

    @Transactional
    public void deleteVaultItem(String tenantId, java.util.UUID vaultItemId) {
        VaultItem vaultItem = vaultItemRepository.findByIdAndTenantId(vaultItemId, tenantId)
                .orElseThrow(() -> new com.yourorg.platform.foculist.resource.clean.domain.model.ResourceDomainException("Vault item not found"));
        vaultItemRepository.delete(vaultItem);
    }

    private BookmarkView toBookmarkView(Bookmark bookmark) {
        return new BookmarkView(
                bookmark.id(),
                bookmark.title(),
                bookmark.url(),
                bookmark.tenantId(),
                bookmark.createdAt()
        );
    }

    private WorklogEntryView toWorklogView(WorklogEntry worklogEntry) {
        return new WorklogEntryView(
                worklogEntry.id(),
                worklogEntry.project(),
                worklogEntry.task(),
                worklogEntry.durationMinutes(),
                worklogEntry.tenantId(),
                worklogEntry.loggedAt()
        );
    }

    private VaultItemView toVaultItemView(VaultItem vaultItem) {
        return new VaultItemView(
                vaultItem.id(),
                vaultItem.name(),
                vaultItem.classification(),
                vaultItem.tenantId(),
                vaultItem.createdAt()
        );
    }
}
