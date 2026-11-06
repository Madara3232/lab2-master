package com.example.lab1.repository;

import com.example.lab1.model.PublicationSlot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;

@Repository
public class PublicationSlotRepository extends JdbcRepository<PublicationSlot> {
    public PublicationSlotRepository(JdbcTemplate jdbc) {
        super(jdbc, (row, number) -> new PublicationSlot(row.getLong("id"),
                row.getLong("podcast_id"), row.getLong("episode_id"), offset(row, "scheduled_at"), row.getString("status"),
                instant(row, "created_at"), instant(row, "updated_at")),
                "publication_slots", "podcast_id", "episode_id", "scheduled_at", "status");
    }

    public PublicationSlot save(Long id, Long podcastId, Long episodeId, OffsetDateTime scheduledAt, String status) {
        return super.save(id, podcastId, episodeId, scheduledAt, status);
    }

    public PublicationSlot changeStatus(PublicationSlot slot, String status) {
        return save(slot.id(), slot.podcastId(), slot.episodeId(), slot.scheduledAt(), status);
    }

    public boolean hasSlots(Long podcastId, Long episodeId) {
        return findOne("podcast_id = ? AND episode_id = ? LIMIT 1", podcastId, episodeId).isPresent();
    }

    public boolean hasScheduledSlots(Long podcastId) {
        return findOne("podcast_id = ? AND status = 'SCHEDULED' LIMIT 1", podcastId).isPresent();
    }

    public int withdrawRelease(Long episodeId) {
        return jdbc.update("""
                UPDATE publication_slots SET status = 'WITHDRAWN', updated_at = clock_timestamp()
                WHERE episode_id = ? AND status = 'PUBLISHED'
                """, episodeId);
    }

    public void withdrawForPodcast(Long podcastId) {
        jdbc.update("""
                UPDATE publication_slots SET status = 'WITHDRAWN', updated_at = clock_timestamp()
                WHERE podcast_id = ? AND status = 'PUBLISHED'
                """, podcastId);
    }

    public int cancelForPodcast(Long podcastId) {
        return jdbc.update("""
                UPDATE publication_slots SET status = 'CANCELLED', updated_at = clock_timestamp()
                WHERE podcast_id = ? AND status = 'SCHEDULED'
                """, podcastId);
    }
}
