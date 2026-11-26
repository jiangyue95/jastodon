CREATE TABLE follows (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    remote_account_id BIGINT NOT NULL UNIQUE REFERENCES remote_accounts (id),
    follow_activity_id TEXT NOT NULL UNIQUE,
    state TEXT NOT NULL CHECK (state IN ('PENDING', 'ACCEPTED')),
    requested_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ
);
