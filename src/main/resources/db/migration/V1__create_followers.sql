CREATE TABLE followers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id TEXT NOT NULL UNIQUE,
    inbox TEXT NOT NULL,
    follow_activity_id TEXT NOT NULL,
    followed_at TIMESTAMPTZ NOT NULL
);
