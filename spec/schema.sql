CREATE TABLE public.feeds (
	id uuid DEFAULT uuidv7() NOT NULL,
	name text NOT NULL,
	url text NOT NULL,
	enabled boolean DEFAULT TRUE NOT NULL,
	CONSTRAINT feeds_pkey PRIMARY KEY (id)
);

CREATE TABLE public.entries (
	id uuid DEFAULT uuidv7() NOT NULL,
	external_id text NOT NULL,
	feed_id uuid NOT NULL,
	url text NOT NULL,
	title text NOT NULL,
	CONSTRAINT entries_pkey PRIMARY KEY (id),
	CONSTRAINT entries_feed_id_fkey FOREIGN KEY (feed_id) REFERENCES public.feeds(id)
);
