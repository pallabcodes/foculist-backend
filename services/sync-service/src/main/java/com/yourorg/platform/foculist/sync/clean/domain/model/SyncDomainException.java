package com.yourorg.platform.foculist.sync.clean.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;

public final class SyncDomainException extends DomainException {
    public SyncDomainException(String message) {
        super("SYNC_DOMAIN_ERROR", message);
    }
}
