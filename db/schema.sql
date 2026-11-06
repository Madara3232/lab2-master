CREATE TABLE users (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL CHECK (name ~ '[^[:space:]]'),
    email text NOT NULL CHECK (email ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_email_ci ON users (lower(email));

CREATE TABLE podcasts (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title text NOT NULL CHECK (title ~ '[^[:space:]]'),
    author_id bigint NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status text NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX podcasts_author_idx ON podcasts (author_id);

CREATE TABLE episodes (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    podcast_id bigint NOT NULL REFERENCES podcasts (id) ON DELETE CASCADE,
    title text NOT NULL CHECK (title ~ '[^[:space:]]'),
    duration_seconds integer NOT NULL CHECK (duration_seconds > 0),
    publication_state text NOT NULL CHECK (publication_state IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (podcast_id, id)
);

CREATE TABLE audio_files (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    episode_id bigint NOT NULL REFERENCES episodes (id) ON DELETE CASCADE,
    path text NOT NULL CHECK (path ~ '[^[:space:]]'),
    format text NOT NULL CHECK (format ~ '[^[:space:]]'),
    size_bytes bigint NOT NULL CHECK (size_bytes > 0),
    is_primary boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audio_files_episode_idx ON audio_files (episode_id);
CREATE UNIQUE INDEX audio_files_one_primary ON audio_files (episode_id) WHERE is_primary;

CREATE TABLE publication_slots (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    podcast_id bigint NOT NULL,
    episode_id bigint NOT NULL,
    scheduled_at timestamptz NOT NULL CHECK (isfinite(scheduled_at)),
    status text NOT NULL CHECK (status IN ('SCHEDULED', 'PUBLISHED', 'CANCELLED', 'WITHDRAWN')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (podcast_id, episode_id) REFERENCES episodes (podcast_id, id) ON DELETE CASCADE
);
CREATE INDEX publication_slots_episode_idx ON publication_slots (podcast_id, episode_id);
CREATE UNIQUE INDEX publication_slots_one_pending ON publication_slots (episode_id) WHERE status = 'SCHEDULED';
CREATE UNIQUE INDEX publication_slots_one_current_release ON publication_slots (episode_id) WHERE status = 'PUBLISHED';
CREATE UNIQUE INDEX publication_slots_podcast_time ON publication_slots (podcast_id, scheduled_at) WHERE status = 'SCHEDULED';
