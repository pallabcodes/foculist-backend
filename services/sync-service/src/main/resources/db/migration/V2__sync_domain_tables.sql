create table if not exists sync_push_envelope (
  id uuid primary key,
  tenant_id varchar(128) not null,
  device_id varchar(128) not null,
  payload_version varchar(64) not null,
  pending_changes integer not null,
  payload jsonb not null,
  client_sync_time timestamptz,
  received_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_sync_push_tenant_device_received
  on sync_push_envelope (tenant_id, device_id, received_at);

create table if not exists sync_change_event (
  id uuid primary key,
  tenant_id varchar(128) not null,
  device_id varchar(128) not null,
  change_type varchar(32) not null,
  payload jsonb not null,
  occurred_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_sync_change_tenant_occurred
  on sync_change_event (tenant_id, occurred_at);

create table if not exists sync_device_cursor (
  id uuid primary key,
  tenant_id varchar(128) not null,
  device_id varchar(128) not null,
  last_client_sync timestamptz,
  last_pull_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create unique index if not exists idx_sync_cursor_tenant_device
  on sync_device_cursor (tenant_id, device_id);
