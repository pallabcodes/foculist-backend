create table if not exists identity_service_bootstrap (
  id uuid primary key,
  tenant_id varchar(128) not null,
  name varchar(128) not null,
  created_at timestamptz not null
);

create index if not exists idx_identity_service_bootstrap_tenant
  on identity_service_bootstrap (tenant_id);

create unique index if not exists uk_identity_service_bootstrap_tenant_name
  on identity_service_bootstrap (tenant_id, name);
