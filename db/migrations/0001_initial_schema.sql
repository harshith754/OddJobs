create extension if not exists pgcrypto;

create table if not exists streams (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    public_token text not null unique,
    status text not null default 'paused' check (status in ('active', 'paused', 'ended', 'error')),
    default_interval_seconds integer not null default 2,
    default_quality text not null default 'high' check (default_quality in ('balanced', 'high', 'max')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists stream_sessions (
    id uuid primary key default gen_random_uuid(),
    stream_id uuid not null references streams(id) on delete cascade,
    status text not null default 'active' check (status in ('active', 'paused', 'ended', 'error')),
    started_at timestamptz not null default now(),
    ended_at timestamptz,
    last_image_at timestamptz,
    created_at timestamptz not null default now()
);

create table if not exists stream_images (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references stream_sessions(id) on delete cascade,
    storage_path text not null,
    sequence_number integer not null,
    width integer not null,
    height integer not null,
    file_size_bytes bigint not null,
    created_at timestamptz not null default now()
);

create index if not exists stream_sessions_stream_id_idx on stream_sessions(stream_id, started_at desc);
create index if not exists stream_images_session_id_idx on stream_images(session_id, sequence_number desc);

insert into streams (name, public_token, status, default_interval_seconds, default_quality)
values ('Main Frame Stream', 'main-frame-stream', 'paused', 2, 'high')
on conflict (public_token) do nothing;

