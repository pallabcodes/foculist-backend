-- V5: Task Items (the core entity of Foculist)
-- Links to projects and supports rich metadata

create table if not exists task_item (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       varchar(128) not null,
    project_id      uuid not null references project_item(id) on delete cascade,
    title           varchar(512) not null,
    description     text,
    status          varchar(64) not null, -- Normalized status ID
    priority        varchar(32) not null default 'MEDIUM',
    type            varchar(32) not null default 'TASK', -- TASK, FEATURE, BUG, EPIC
    story_points    integer,
    assignee_id     uuid,
    reporter_id     uuid not null,
    due_date        date,
    created_at      timestamptz not null default current_timestamp,
    updated_at      timestamptz not null default current_timestamp,
    created_by      varchar(255),
    updated_by      varchar(255),
    deleted_at      timestamptz,
    version         bigint not null default 0,
    metadata        jsonb default '{}'
);

-- Optimization indices
create index if not exists idx_task_item_project on task_item (tenant_id, project_id);
create index if not exists idx_task_item_assignee on task_item (tenant_id, assignee_id);
create index if not exists idx_task_item_reporter on task_item (tenant_id, reporter_id);
create index if not exists idx_task_item_status on task_item (project_id, status);
create index if not exists idx_task_item_deleted_at on task_item (deleted_at) where deleted_at is null;
