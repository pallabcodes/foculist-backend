create table if not exists project_item (
  id uuid primary key,
  tenant_id varchar(128) not null,
  name varchar(255) not null,
  description text,
  status varchar(32) not null,
  priority varchar(32) not null,
  due_date date,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_project_item_tenant
  on project_item (tenant_id);

create index if not exists idx_project_item_tenant_status
  on project_item (tenant_id, status);

create unique index if not exists uk_project_item_tenant_name
  on project_item (tenant_id, name);

create unique index if not exists uk_project_item_id_tenant
  on project_item (id, tenant_id);

create table if not exists project_settings (
  project_id uuid primary key,
  tenant_id varchar(128) not null,
  default_view varchar(32) not null,
  updated_at timestamptz not null,
  version bigint not null default 0,
  constraint fk_project_settings_project
    foreign key (project_id, tenant_id)
    references project_item (id, tenant_id)
    on delete cascade
);

create index if not exists idx_project_settings_tenant
  on project_settings (tenant_id);

create table if not exists project_workflow_status (
  project_id uuid not null references project_settings(project_id) on delete cascade,
  position integer not null,
  status varchar(64) not null,
  primary key (project_id, position)
);

create index if not exists idx_project_workflow_status_project
  on project_workflow_status (project_id);
