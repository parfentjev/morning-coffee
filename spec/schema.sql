create table public.feeds
(
    id uuid default uuidv7() not null,
    name text not null,
    url text not null,
    enabled boolean default true not null,
    request_timeout_seconds smallint default 10 not null check(request_timeout_seconds > 0),
    constraint feeds_pkey primary key (id)
);

CREATE TABLE public.entries
(
    id           uuid DEFAULT uuidv7() NOT NULL,
    external_id  text                  NOT NULL,
    published_at timestamptz           NOT NULL,
    feed_id      uuid                  NOT NULL,
    title        text                  NOT NULL,
    link         text                  NOT NULL,
    CONSTRAINT entries_pkey PRIMARY KEY (id),
    CONSTRAINT entries_feed_id_fkey FOREIGN KEY (feed_id) REFERENCES public.feeds (id),
    CONSTRAINT entries_external_id_feed_id_key UNIQUE (external_id, feed_id)
);
