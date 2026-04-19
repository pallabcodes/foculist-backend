CREATE TABLE meeting_outbox_event (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_meeting_outbox_status_occurred ON meeting_outbox_event (status, occurred_at);
CREATE INDEX idx_meeting_outbox_tenant ON meeting_outbox_event (tenant_id);
CREATE INDEX idx_meeting_outbox_aggregate ON meeting_outbox_event (aggregate_type, aggregate_id);
