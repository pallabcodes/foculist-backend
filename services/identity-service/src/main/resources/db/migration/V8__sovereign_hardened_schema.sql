-- Hardening core schemas for Google-grade auditability, soft-deletes, and extensibility

-- Organizations
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- Users
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(45);

-- Memberships
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE memberships ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';

-- Add indices for soft-delete filtering
CREATE INDEX IF NOT EXISTS idx_organizations_deleted_at ON organizations(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_memberships_deleted_at ON memberships(deleted_at) WHERE deleted_at IS NULL;
