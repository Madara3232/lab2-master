\set ON_ERROR_STOP on
\getenv app_password PODCAST_APP_PASSWORD

CREATE ROLE podcast_app LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;
CREATE DATABASE podcast_catalog;
REVOKE ALL ON DATABASE podcast_catalog FROM PUBLIC;
GRANT CONNECT ON DATABASE podcast_catalog TO podcast_app;

\connect podcast_catalog
BEGIN;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO podcast_app;
\ir schema.sql
\ir seed.sql
\ir security.sql
GRANT SELECT, INSERT, UPDATE, DELETE ON users, podcasts, episodes, audio_files, publication_slots TO podcast_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO podcast_app;
COMMIT;
