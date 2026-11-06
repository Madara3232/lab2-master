CREATE TABLE IF NOT EXISTS accounts (
    user_id bigint PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    password_hash text NOT NULL CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'),
    role text NOT NULL DEFAULT 'READER' CHECK (role IN ('READER', 'EDITOR', 'ADMIN')),
    enabled boolean NOT NULL DEFAULT true
);
GRANT SELECT, INSERT ON accounts TO podcast_app;
GRANT UPDATE (role, enabled) ON accounts TO podcast_app;
