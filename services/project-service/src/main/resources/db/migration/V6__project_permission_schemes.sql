-- V6: Project Permission Schemes (JIRA-style matrix)

create table if not exists project_permission_scheme (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       varchar(128) not null,
    name            varchar(255) not null,
    description     text,
    actions_mapping jsonb not null default '{}', -- role_id -> list of allowed actions
    created_at      timestamptz not null default current_timestamp,
    updated_at      timestamptz not null default current_timestamp
);

create index if not exists idx_project_permission_scheme_tenant on project_permission_scheme (tenant_id);

-- Enforce Permission Schemes on Projects
alter table project_item 
    add column if not exists permission_scheme_id uuid references project_permission_scheme(id);

-- Seed a Default Scheme
insert into project_permission_scheme (id, tenant_id, name, actions_mapping)
values (
    'd0000000-0000-0000-0000-000000000001', 
    'system', 
    'Standard Agile Scheme', 
    '{
        "ADMIN": ["project:edit", "project:delete", "task:create", "task:edit", "task:delete", "task:transition", "task:assign"],
        "DEVELOPER": ["task:create", "task:edit", "task:transition", "task:assign"],
        "VIEWER": ["project:view", "task:view"],
        "REPORTER": ["task:edit", "task:comment"],
        "ASSIGNEE": ["task:edit", "task:transition"]
    }'::jsonb
) on conflict do nothing;
