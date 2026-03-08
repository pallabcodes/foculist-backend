create table if not exists calendar_event (
  id uuid primary key,
  tenant_id varchar(128) not null,
  title varchar(255) not null,
  event_date date not null,
  event_time time not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_calendar_event_tenant_date_time
  on calendar_event (tenant_id, event_date, event_time);

create table if not exists calendar_agenda_context (
  id uuid primary key,
  tenant_id varchar(128) not null,
  meeting_id varchar(128) not null,
  title varchar(255) not null,
  start_time time,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create unique index if not exists idx_calendar_agenda_tenant_meeting
  on calendar_agenda_context (tenant_id, meeting_id);
