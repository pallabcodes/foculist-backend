package com.yourorg.platform.foculist.identity.clean.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "token_blacklist")
public class TokenBlacklist {

    @Id
    @Column(name = "jti", nullable = false, length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    protected TokenBlacklist() {}

    public TokenBlacklist(String jti, Instant expiresAt, Instant revokedAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public String getJti() { return jti; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
