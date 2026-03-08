create table if not exists project_service_bootstrap (
  id uuid primary key,
  tenant_id varchar(128) not null,
  name varchar(128) not null,
  created_at timestamptz not null
);

create index if not exists idx_project_service_bootstrap_tenant
  on project_service_bootstrap (tenant_id);

create unique index if not exists uk_project_service_bootstrap_tenant_name
  on project_service_bootstrap (tenant_id, name);
