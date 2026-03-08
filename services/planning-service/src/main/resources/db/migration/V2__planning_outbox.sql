create table if not exists planning_outbox_event (
  id uuid primary key,
  tenant_id varchar(128) not null,
  aggregate_type varchar(128) not null,
  aggregate_id uuid not null,
  event_type varchar(128) not null,
  payload jsonb not null,
  status varchar(32) not null,
  occurred_at timestamptz not null,
  published_at timestamptz,
  attempts integer not null default 0,
  last_error text,
  version bigint not null default 0
);

create index if not exists idx_planning_outbox_status_occurred
  on planning_outbox_event (status, occurred_at);

create index if not exists idx_planning_outbox_tenant
  on planning_outbox_event (tenant_id);

create index if not exists idx_planning_outbox_aggregate
  on planning_outbox_event (aggregate_type, aggregate_id);
