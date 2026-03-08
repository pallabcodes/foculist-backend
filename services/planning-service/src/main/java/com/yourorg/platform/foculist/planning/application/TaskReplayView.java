package com.yourorg.platform.foculist.planning.application;

import java.time.Instant;

public record TaskReplayView(
        TaskView task,
        Instant replayedAt,
        Long snapshotVersionUsed,
        long versionReplayed
) {
}
