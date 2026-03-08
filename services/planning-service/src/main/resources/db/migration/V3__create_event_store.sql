create table if not exists planning_events (
    id uuid primary key,
    tenant_id varchar(128) not null,
    aggregate_id uuid not null,
    aggregate_type varchar(64) not null,
    event_type varchar(128) not null,
    payload jsonb not null,
    version bigint not null,
    occurred_at timestamptz not null
);

create index if not exists idx_planning_events_aggregate
  on planning_events (aggregate_id, aggregate_type);

create index if not exists idx_planning_events_tenant_aggregate
  on planning_events (tenant_id, aggregate_id, aggregate_type);

create unique index if not exists uk_planning_events_aggregate_version
  on planning_events (aggregate_id, aggregate_type, version);
