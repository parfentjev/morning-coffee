CREATE TABLE public.feeds (
	id uuid DEFAULT uuidv7() NOT NULL,
	url text NOT NULL
);

CREATE TABLE public.entries (
	id uuid DEFAULT uuidv7() NOT NULL,
	external_id text NOT NULL,
	url text NOT NULL,
	title text NOT NULL
);
