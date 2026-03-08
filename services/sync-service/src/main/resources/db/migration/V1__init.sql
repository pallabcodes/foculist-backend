create table if not exists sync_service_bootstrap (
  id uuid primary key,
  tenant_id varchar(128) not null,
  name varchar(128) not null,
  created_at timestamptz not null
);

create index if not exists idx_sync_service_bootstrap_tenant
  on sync_service_bootstrap (tenant_id);

create unique index if not exists uk_sync_service_bootstrap_tenant_name
  on sync_service_bootstrap (tenant_id, name);
