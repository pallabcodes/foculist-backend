-- Hardening Project schemas for Google-grade auditability, soft-deletes, and extensibility

-- Project Items
ALTER TABLE project_item ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE project_item ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE project_item ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE project_item ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- Project Settings
ALTER TABLE project_settings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE project_settings ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE project_settings ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE project_settings ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

-- Indices for soft-delete filtering
CREATE INDEX IF NOT EXISTS idx_project_item_deleted_at ON project_item(deleted_at) WHERE deleted_at IS NULL;
