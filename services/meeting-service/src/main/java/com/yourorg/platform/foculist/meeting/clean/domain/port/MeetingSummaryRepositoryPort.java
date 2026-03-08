package com.yourorg.platform.foculist.meeting.clean.domain.port;

import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingSummary;
import java.util.List;

public interface MeetingSummaryRepositoryPort {
    List<MeetingSummary> findByTenantId(String tenantId, int page, int size);

    MeetingSummary save(MeetingSummary summary);
}
