create table if not exists resource_bookmark (
  id uuid primary key,
  tenant_id varchar(128) not null,
  title varchar(255) not null,
  url text not null,
  created_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_resource_bookmark_tenant_created
  on resource_bookmark (tenant_id, created_at desc);

create unique index if not exists uk_resource_bookmark_tenant_url
  on resource_bookmark (tenant_id, url);

create table if not exists resource_worklog_entry (
  id uuid primary key,
  tenant_id varchar(128) not null,
  project_name varchar(255) not null,
  task_name varchar(255) not null,
  duration_minutes integer not null check (duration_minutes > 0 and duration_minutes <= 1440),
  logged_at timestamptz not null,
  created_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_resource_worklog_tenant_logged
  on resource_worklog_entry (tenant_id, logged_at desc);

create table if not exists resource_vault_item (
  id uuid primary key,
  tenant_id varchar(128) not null,
  name varchar(255) not null,
  classification varchar(32) not null,
  created_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_resource_vault_tenant
  on resource_vault_item (tenant_id);

create unique index if not exists uk_resource_vault_tenant_name
  on resource_vault_item (tenant_id, name);
