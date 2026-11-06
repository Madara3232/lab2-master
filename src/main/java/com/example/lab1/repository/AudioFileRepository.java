package com.example.lab1.repository;

import com.example.lab1.model.AudioFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class AudioFileRepository extends JdbcRepository<AudioFile> {
    public AudioFileRepository(JdbcTemplate jdbc) {
        super(jdbc, (row, number) -> new AudioFile(row.getLong("id"),
                row.getLong("episode_id"), row.getString("path"), row.getString("format"), row.getLong("size_bytes"), row.getBoolean("is_primary"),
                instant(row, "created_at"), instant(row, "updated_at")),
                "audio_files", "episode_id", "path", "format", "size_bytes", "is_primary");
    }

    public AudioFile save(Long id, Long episodeId, String path, String format, long sizeBytes, boolean primary) {
        return super.save(id, episodeId, path, format, sizeBytes, primary);
    }

    public Optional<AudioFile> findPrimary(Long episodeId) {
        return findOne("episode_id = ? AND is_primary", episodeId);
    }

    public AudioFile changePrimary(AudioFile file, boolean primary) {
        return save(file.id(), file.episodeId(), file.path(), file.format(), file.sizeBytes(), primary);
    }
}
