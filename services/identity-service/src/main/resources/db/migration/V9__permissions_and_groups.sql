-- V9: IAM Permissions, Roles, and User Groups for Google-grade RBAC
-- These tables provide fine-grained, auditable access control.

-- 1. Permissions — Atomic permission definitions
CREATE TABLE IF NOT EXISTS permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(128) NOT NULL,
    code        VARCHAR(100) NOT NULL,   -- e.g. 'project:read', 'sprint:admin'
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(50),             -- e.g. 'PROJECT', 'PLANNING', 'IDENTITY'
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    deleted_at  TIMESTAMPTZ,
    metadata    JSONB DEFAULT '{}'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_permissions_tenant_code ON permissions (tenant_id, code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_permissions_tenant ON permissions (tenant_id);
CREATE INDEX IF NOT EXISTS idx_permissions_category ON permissions (tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_permissions_deleted_at ON permissions (deleted_at) WHERE deleted_at IS NULL;

-- 2. Roles — Named role bundles
CREATE TABLE IF NOT EXISTS roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(128) NOT NULL,
    code        VARCHAR(50) NOT NULL,    -- e.g. 'OWNER', 'ADMIN', 'MEMBER', 'VIEWER'
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,  -- system roles cannot be deleted
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    deleted_at  TIMESTAMPTZ,
    metadata    JSONB DEFAULT '{}'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_roles_tenant_code ON roles (tenant_id, code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles (tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_deleted_at ON roles (deleted_at) WHERE deleted_at IS NULL;

-- 3. Role-Permission join
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by    VARCHAR(255),
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions (role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions (permission_id);

-- 4. User Groups — Named groups within a tenant
CREATE TABLE IF NOT EXISTS user_groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(128) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    deleted_at  TIMESTAMPTZ,
    metadata    JSONB DEFAULT '{}'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_groups_tenant_name ON user_groups (tenant_id, name) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_groups_tenant ON user_groups (tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_groups_deleted_at ON user_groups (deleted_at) WHERE deleted_at IS NULL;

-- 5. User Group Members — M:N join
CREATE TABLE IF NOT EXISTS user_group_members (
    group_id   UUID NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    joined_by  VARCHAR(255),
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_group_members_group ON user_group_members (group_id);
CREATE INDEX IF NOT EXISTS idx_user_group_members_user ON user_group_members (user_id);

-- 6. User-Role assignment (direct role assignment to users within a tenant)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    tenant_id  VARCHAR(128) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    PRIMARY KEY (user_id, role_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles (role_id);
