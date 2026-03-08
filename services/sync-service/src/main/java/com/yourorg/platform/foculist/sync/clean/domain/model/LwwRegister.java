package com.yourorg.platform.foculist.sync.clean.domain.model;

import java.time.Instant;

/**
 * A Last-Writer-Wins (LWW) register — the simplest convergent CRDT.
 * <p>
 * Each write carries a wall-clock timestamp and a writer ID. When two
 * replicas merge, the register with the later timestamp wins. Ties
 * are broken lexicographically by writer ID for deterministic convergence.
 * <p>
 * This is the building block for field-level conflict resolution in
 * the sync service.
 *
 * @param value     the current value
 * @param timestamp wall-clock time of the last write
 * @param writerId  unique identifier of the writer (userId or deviceId)
 * @param <T>       the value type
 */
public record LwwRegister<T>(T value, Instant timestamp, String writerId) {

    public LwwRegister {
        if (timestamp == null) {
            throw new SyncDomainException("LWW register timestamp is required");
        }
        if (writerId == null || writerId.isBlank()) {
            throw new SyncDomainException("LWW register writerId is required");
        }
    }

    /**
     * Merge this register with a remote one.
     * <p>
     * Returns the register with the later timestamp. If timestamps are
     * equal, the register with the lexicographically greater writerId wins
     * (deterministic tie-breaking for convergence).
     *
     * @param remote the remote register to merge with
     * @return the winning register
     */
    public LwwRegister<T> merge(LwwRegister<T> remote) {
        if (remote == null) {
            return this;
        }

        int cmp = this.timestamp.compareTo(remote.timestamp);
        if (cmp > 0) {
            return this; // Local is newer
        } else if (cmp < 0) {
            return remote; // Remote is newer
        } else {
            // Tie: deterministic tiebreak by writerId
            return this.writerId.compareTo(remote.writerId) >= 0 ? this : remote;
        }
    }

    /**
     * Create a new register with an updated value and current timestamp.
     */
    public LwwRegister<T> write(T newValue, Instant now, String newWriterId) {
        return new LwwRegister<>(newValue, now, newWriterId);
    }
}
