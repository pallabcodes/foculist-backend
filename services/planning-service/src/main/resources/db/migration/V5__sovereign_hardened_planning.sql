-- Hardening Planning schemas for Google-grade auditability, soft-deletes, and extensibility

-- Hardening Sprint Table
ALTER TABLE planning_sprint
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_planning_sprint_deleted_at ON planning_sprint (deleted_at) WHERE deleted_at IS NULL;

-- Hardening Task Table
ALTER TABLE planning_task
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_planning_task_deleted_at ON planning_task (deleted_at) WHERE deleted_at IS NULL;

-- Hardening Snapshot Table
ALTER TABLE planning_task_snapshot
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_planning_task_snapshot_deleted_at ON planning_task_snapshot (deleted_at) WHERE deleted_at IS NULL;

-- Hardening Projection Table
ALTER TABLE planning_task_projection
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_planning_task_projection_deleted_at ON planning_task_projection (deleted_at) WHERE deleted_at IS NULL;
