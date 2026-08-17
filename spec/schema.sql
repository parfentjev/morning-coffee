create table public.feeds
(
    id uuid default uuidv7() not null,
    name text not null,
    url text not null,
    enabled boolean default true not null,
    request_timeout_seconds smallint default 10 not null check(request_timeout_seconds > 0),
    constraint feeds_pkey primary key (id)
);

create table public.entries
(
    id uuid default uuidv7() not null,
    external_id text not null,
    published_at timestamptz not null,
    feed_id uuid not null,
    title text not null,
    link text not null,
    constraint entries_pkey primary key (id),
    constraint entries_feed_id_fkey foreign key (feed_id) references public.feeds (id),
    constraint entries_external_id_feed_id_key unique (external_id, feed_id)
);
