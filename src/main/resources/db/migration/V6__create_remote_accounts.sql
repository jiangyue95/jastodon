CREATE TABLE remote_accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id TEXT NOT NULL UNIQUE,
    acct TEXT NOT NULL,
    inbox TEXT NOT NULL,
    name TEXT,
    avatar_url TEXT,
    fetched_at TIMESTAMPTZ NOT NULL
);
