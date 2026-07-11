create table users (
    id            uuid primary key default gen_random_uuid(),
    email         varchar(255) not null unique,
    password_hash varchar(255) not null,
    role          varchar(20) not null default 'USER' check (role in ('USER', 'ADMIN')),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

create table projects (
    id          uuid primary key default gen_random_uuid(),
    name        varchar(200) not null,
    description varchar(2000),
    owner_id    uuid not null references users (id),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table tasks (
    id          uuid primary key default gen_random_uuid(),
    project_id  uuid not null references projects (id) on delete cascade,
    title       varchar(200) not null,
    description varchar(2000),
    status      varchar(20) not null default 'TODO',
    due_date    date,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table tags (
    id         uuid primary key default gen_random_uuid(),
    name       varchar(100) not null unique,
    created_at timestamptz not null default now()
);

create table task_tags (
    task_id uuid not null references tasks (id) on delete cascade,
    tag_id  uuid not null references tags (id) on delete cascade,
    primary key (task_id, tag_id)
);

create index idx_projects_owner_id on projects (owner_id);
create index idx_tasks_project_id on tasks (project_id);
create index idx_task_tags_tag_id on task_tags (tag_id);
