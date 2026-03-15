-- V4: Project Members and Project Key enrichment
-- Links users to projects with role-based access.

-- 1. Enrich project_item with owner and project key
ALTER TABLE project_item
    ADD COLUMN IF NOT EXISTS owner_id UUID,
    ADD COLUMN IF NOT EXISTS key VARCHAR(10);

CREATE UNIQUE INDEX IF NOT EXISTS uk_project_item_tenant_key ON project_item (tenant_id, key) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_project_item_owner ON project_item (tenant_id, owner_id);

-- 2. Project Members (role-based access per project)
CREATE TABLE IF NOT EXISTS project_member (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(128) NOT NULL,
    project_id  UUID NOT NULL REFERENCES project_item(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- OWNER, ADMIN, MEMBER, VIEWER
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    deleted_at  TIMESTAMPTZ,
    metadata    JSONB DEFAULT '{}',
    UNIQUE (project_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_project_member_project ON project_member (tenant_id, project_id);
CREATE INDEX IF NOT EXISTS idx_project_member_user ON project_member (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_project_member_deleted ON project_member (deleted_at) WHERE deleted_at IS NULL;
