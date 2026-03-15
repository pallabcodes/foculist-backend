-- V6: Comments, Labels, Assignees, and Watchers for planning tasks
-- Brings task management to parity with Jira/Linear standards.

-- 1. Enrich planning_task with assignee, reporter, story points, due date
ALTER TABLE planning_task
    ADD COLUMN IF NOT EXISTS assignee_id UUID,
    ADD COLUMN IF NOT EXISTS reporter_id UUID,
    ADD COLUMN IF NOT EXISTS story_points INTEGER,
    ADD COLUMN IF NOT EXISTS due_date DATE,
    ADD COLUMN IF NOT EXISTS task_key VARCHAR(20);   -- e.g. 'MARS-42'

CREATE INDEX IF NOT EXISTS idx_planning_task_assignee ON planning_task (tenant_id, assignee_id);
CREATE INDEX IF NOT EXISTS idx_planning_task_reporter ON planning_task (tenant_id, reporter_id);
CREATE INDEX IF NOT EXISTS idx_planning_task_key ON planning_task (tenant_id, task_key);

-- 2. Task Comments (threaded)
CREATE TABLE IF NOT EXISTS planning_task_comment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(128) NOT NULL,
    task_id     UUID NOT NULL REFERENCES planning_task(id) ON DELETE CASCADE,
    parent_id   UUID REFERENCES planning_task_comment(id) ON DELETE SET NULL,
    author_id   UUID NOT NULL,
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    deleted_at  TIMESTAMPTZ,
    metadata    JSONB DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_task_comment_task ON planning_task_comment (tenant_id, task_id, created_at);
CREATE INDEX IF NOT EXISTS idx_task_comment_author ON planning_task_comment (tenant_id, author_id);
CREATE INDEX IF NOT EXISTS idx_task_comment_parent ON planning_task_comment (parent_id);
CREATE INDEX IF NOT EXISTS idx_task_comment_deleted ON planning_task_comment (deleted_at) WHERE deleted_at IS NULL;

-- 3. Labels (tenant-scoped)
CREATE TABLE IF NOT EXISTS planning_label (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(128) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    color       VARCHAR(7) NOT NULL DEFAULT '#6B7280',  -- hex color
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    deleted_at  TIMESTAMPTZ,
    metadata    JSONB DEFAULT '{}'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_planning_label_tenant_name ON planning_label (tenant_id, name) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_planning_label_tenant ON planning_label (tenant_id);
CREATE INDEX IF NOT EXISTS idx_planning_label_deleted ON planning_label (deleted_at) WHERE deleted_at IS NULL;

-- 4. Task-Label join (M:N)
CREATE TABLE IF NOT EXISTS planning_task_label (
    task_id  UUID NOT NULL REFERENCES planning_task(id) ON DELETE CASCADE,
    label_id UUID NOT NULL REFERENCES planning_label(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(255),
    PRIMARY KEY (task_id, label_id)
);

CREATE INDEX IF NOT EXISTS idx_task_label_task ON planning_task_label (task_id);
CREATE INDEX IF NOT EXISTS idx_task_label_label ON planning_task_label (label_id);

-- 5. Task Assignees (M:N — tasks can have multiple assignees)
CREATE TABLE IF NOT EXISTS planning_task_assignee (
    task_id     UUID NOT NULL REFERENCES planning_task(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    PRIMARY KEY (task_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_task_assignee_task ON planning_task_assignee (task_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee_user ON planning_task_assignee (user_id);

-- 6. Task Watchers (users subscribed to notifications)
CREATE TABLE IF NOT EXISTS planning_task_watcher (
    task_id    UUID NOT NULL REFERENCES planning_task(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL,
    watched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_task_watcher_task ON planning_task_watcher (task_id);
CREATE INDEX IF NOT EXISTS idx_task_watcher_user ON planning_task_watcher (user_id);
