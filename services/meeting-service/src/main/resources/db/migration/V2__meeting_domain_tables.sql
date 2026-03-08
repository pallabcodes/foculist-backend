create table if not exists meeting_summary (
  id uuid primary key,
  tenant_id varchar(128) not null,
  meeting_id varchar(128) not null,
  content text not null,
  style varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  version bigint not null default 0
);

create index if not exists idx_meeting_summary_tenant_created
  on meeting_summary (tenant_id, created_at desc);

create index if not exists idx_meeting_summary_tenant_meeting
  on meeting_summary (tenant_id, meeting_id);
