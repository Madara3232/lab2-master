WITH author AS (
    INSERT INTO users (name, email) VALUES ('Demo author', 'seed-author@example.test') RETURNING id
), podcast AS (
    INSERT INTO podcasts (title, author_id, status)
    SELECT 'Sound diary', id, 'ACTIVE' FROM author RETURNING id
), episode AS (
    INSERT INTO episodes (podcast_id, title, duration_seconds, publication_state)
    SELECT id, 'Pilot', 300, 'DRAFT' FROM podcast RETURNING id, podcast_id
), audio AS (
    INSERT INTO audio_files (episode_id, path, format, size_bytes, is_primary)
    SELECT id, 'demo/pilot.mp3', 'mp3', 4000000, true FROM episode
    UNION ALL
    SELECT id, 'demo/pilot.ogg', 'ogg', 3500000, false FROM episode
)
INSERT INTO publication_slots (podcast_id, episode_id, scheduled_at, status)
SELECT podcast_id, id, now() - interval '1 hour', 'SCHEDULED' FROM episode;

WITH author AS (
    INSERT INTO users (name, email) VALUES ('Second author', 'seed-second@example.test') RETURNING id
), podcast AS (
    INSERT INTO podcasts (title, author_id, status)
    SELECT 'Stories from the road', id, 'ACTIVE' FROM author RETURNING id
), episode AS (
    INSERT INTO episodes (podcast_id, title, duration_seconds, publication_state)
    SELECT id, 'First stop', 540, 'DRAFT' FROM podcast RETURNING id, podcast_id
), audio AS (
    INSERT INTO audio_files (episode_id, path, format, size_bytes, is_primary)
    SELECT id, 'demo/first-stop.mp3', 'mp3', 7200000, true FROM episode
)
INSERT INTO publication_slots (podcast_id, episode_id, scheduled_at, status)
SELECT podcast_id, id, now() + interval '1 day', 'SCHEDULED' FROM episode;
