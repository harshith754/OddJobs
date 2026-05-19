insert into storage.buckets (id, name, public)
values ('stream-images', 'stream-images', true)
on conflict (id) do update
set public = excluded.public;

