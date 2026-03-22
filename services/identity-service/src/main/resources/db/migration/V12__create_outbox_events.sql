-- Create Transactional Outbox Events Table for Async Processing
CREATE TABLE IF NOT EXISTS identity.outbox_events (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Indices for performance
CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created ON identity.outbox_events (status, created_at) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX IF NOT EXISTS idx_outbox_events_tenant ON identity.outbox_events (tenant_id);
