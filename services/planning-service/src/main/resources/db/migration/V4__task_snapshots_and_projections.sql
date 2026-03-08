create table if not exists planning_task_snapshot (
  id uuid primary key,
  tenant_id varchar(128) not null,
  task_id uuid not null,
  sprint_id uuid,
  title varchar(255) not null,
  description text,
  status varchar(32) not null,
  priority varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null,
  snapshotted_at timestamptz not null
);

create index if not exists idx_planning_task_snapshot_task_version
  on planning_task_snapshot (tenant_id, task_id, version desc);

create table if not exists planning_task_projection (
  id uuid primary key,
  tenant_id varchar(128) not null,
  sprint_id uuid,
  title varchar(255) not null,
  description text,
  status varchar(32) not null,
  priority varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null
);

create index if not exists idx_planning_task_projection_tenant
  on planning_task_projection (tenant_id);

create index if not exists idx_planning_task_projection_tenant_created
  on planning_task_projection (tenant_id, created_at desc, id desc);

create table if not exists planning_projection_checkpoint (
  projection_name varchar(128) primary key,
  last_occurred_at timestamptz,
  last_event_id uuid,
  updated_at timestamptz
);

create table if not exists planning_task_snapshot_job (
  id uuid primary key,
  tenant_id varchar(128) not null,
  task_id uuid not null,
  target_version bigint not null,
  status varchar(32) not null,
  attempts integer not null default 0,
  last_error text,
  created_at timestamptz not null,
  processed_at timestamptz,
  version bigint not null default 0
);

create unique index if not exists uk_planning_snapshot_job_task_target
  on planning_task_snapshot_job (tenant_id, task_id, target_version);

create index if not exists idx_planning_snapshot_job_status_created
  on planning_task_snapshot_job (status, created_at);

create index if not exists idx_planning_outbox_occurred_id
  on planning_outbox_event (occurred_at, id);
