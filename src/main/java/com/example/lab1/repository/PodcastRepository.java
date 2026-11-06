package com.example.lab1.repository;

import com.example.lab1.model.Podcast;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PodcastRepository extends JdbcRepository<Podcast> {
    public PodcastRepository(JdbcTemplate jdbc) {
        super(jdbc, (row, number) -> new Podcast(row.getLong("id"),
                row.getString("title"), row.getLong("author_id"), row.getString("status"),
                instant(row, "created_at"), instant(row, "updated_at")),
                "podcasts", "title", "author_id", "status");
    }

    public Podcast save(Long id, String title, Long authorId, String status) {
        return super.save(id, title, authorId, status);
    }
}
