CREATE TABLE resource_grants (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(36) NOT NULL,
    permission_level VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, resource_type, resource_id)
);

CREATE INDEX idx_resource_grants_tenant ON resource_grants(tenant_id);
CREATE INDEX idx_resource_grants_user_res ON resource_grants(user_id, resource_type, resource_id);
