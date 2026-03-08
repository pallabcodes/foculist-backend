create table if not exists planning_sprint (
  id uuid primary key,
  tenant_id varchar(128) not null,
  name varchar(128) not null,
  status varchar(32) not null,
  start_date timestamptz not null,
  end_date timestamptz not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_planning_sprint_tenant
  on planning_sprint (tenant_id);

create index if not exists idx_planning_sprint_tenant_status
  on planning_sprint (tenant_id, status);

create unique index if not exists uk_planning_sprint_tenant_name
  on planning_sprint (tenant_id, name);

create table if not exists planning_task (
  id uuid primary key,
  tenant_id varchar(128) not null,
  sprint_id uuid references planning_sprint(id),
  title varchar(255) not null,
  description text,
  status varchar(32) not null,
  priority varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_planning_task_tenant
  on planning_task (tenant_id);

create index if not exists idx_planning_task_tenant_sprint
  on planning_task (tenant_id, sprint_id);

create index if not exists idx_planning_task_tenant_status
  on planning_task (tenant_id, status);
