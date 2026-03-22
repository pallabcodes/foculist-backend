-- planning_board
create table if not exists planning_board (
    id uuid primary key,
    tenant_id varchar(128) not null,
    project_id uuid,
    name varchar(128) not null,
    type varchar(32) not null, -- SCRUM or KANBAN
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by varchar(128),
    updated_by varchar(128),
    deleted_at timestamptz,
    metadata jsonb,
    version bigint not null default 0
);

create index if not exists idx_planning_board_tenant on planning_board (tenant_id);
create index if not exists idx_planning_board_tenant_project on planning_board (tenant_id, project_id);
create index if not exists idx_planning_board_deleted_at on planning_board (deleted_at);

-- planning_board_column
create table if not exists planning_board_column (
    id uuid primary key,
    tenant_id varchar(128) not null,
    board_id uuid not null constraint fk_planning_col_board references planning_board(id),
    name varchar(128) not null,
    status_mapping varchar(128), -- e.g., 'TO_DO,IN_PROGRESS'
    order_index integer not null default 0,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by varchar(128),
    updated_by varchar(128),
    deleted_at timestamptz,
    version bigint not null default 0
);

create index if not exists idx_planning_board_column_board on planning_board_column (board_id);
create index if not exists idx_planning_board_column_deleted_at on planning_board_column (deleted_at);

-- planning_epic
create table if not exists planning_epic (
    id uuid primary key,
    tenant_id varchar(128) not null,
    project_id uuid,
    name varchar(255) not null,
    summary text,
    color varchar(32),
    status varchar(32) not null,
    start_date timestamptz,
    target_date timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    created_by varchar(128),
    updated_by varchar(128),
    deleted_at timestamptz,
    metadata jsonb,
    version bigint not null default 0
);

create index if not exists idx_planning_epic_tenant on planning_epic (tenant_id);
create index if not exists idx_planning_epic_tenant_project on planning_epic (tenant_id, project_id);
create index if not exists idx_planning_epic_deleted_at on planning_epic (deleted_at);

-- Add tracking columns to task_projection
alter table planning_task_projection add column if not exists epic_id uuid constraint fk_planning_proj_epic references planning_epic(id);
alter table planning_task_projection add column if not exists board_column_id uuid constraint fk_planning_proj_col references planning_board_column(id);

create index if not exists idx_planning_task_projection_epic on planning_task_projection (epic_id);
create index if not exists idx_planning_task_projection_board_col on planning_task_projection (board_column_id);

-- Add tracking columns to task_snapshots
alter table planning_task_snapshot add column if not exists epic_id uuid;
alter table planning_task_snapshot add column if not exists board_column_id uuid;
