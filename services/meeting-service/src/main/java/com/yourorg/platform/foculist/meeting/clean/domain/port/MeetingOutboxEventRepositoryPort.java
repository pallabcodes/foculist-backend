package com.yourorg.platform.foculist.meeting.clean.domain.port;

import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingOutboxEvent;


import java.util.List;

public interface MeetingOutboxEventRepositoryPort {
    void save(MeetingOutboxEvent event);
    void saveAll(List<MeetingOutboxEvent> events);
    List<MeetingOutboxEvent> findPendingEvents(int limit);
}
