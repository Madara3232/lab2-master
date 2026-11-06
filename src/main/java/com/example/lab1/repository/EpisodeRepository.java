package com.example.lab1.repository;

import com.example.lab1.model.Episode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EpisodeRepository extends JdbcRepository<Episode> {
    public EpisodeRepository(JdbcTemplate jdbc) {
        super(jdbc, (row, number) -> new Episode(row.getLong("id"),
                row.getLong("podcast_id"), row.getString("title"), row.getInt("duration_seconds"), row.getString("publication_state"),
                instant(row, "created_at"), instant(row, "updated_at")),
                "episodes", "podcast_id", "title", "duration_seconds", "publication_state");
    }

    public Episode save(Long id, Long podcastId, String title, int durationSeconds, String publicationState) {
        return super.save(id, podcastId, title, durationSeconds, publicationState);
    }

    public Episode changeState(Episode episode, String state) {
        return save(episode.id(), episode.podcastId(), episode.title(), episode.durationSeconds(), state);
    }

    public boolean hasPublishedEpisodes(Long podcastId) {
        return findOne("podcast_id = ? AND publication_state = 'PUBLISHED' LIMIT 1", podcastId).isPresent();
    }

    public int withdrawForPodcast(Long podcastId) {
        return jdbc.update("""
                UPDATE episodes SET publication_state = 'UNPUBLISHED', updated_at = clock_timestamp()
                WHERE podcast_id = ? AND publication_state = 'PUBLISHED'
                """, podcastId);
    }
}
