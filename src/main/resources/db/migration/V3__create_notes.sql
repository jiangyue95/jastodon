CREATE TABLE notes (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    published TIMESTAMPTZ NOT NULL
);
