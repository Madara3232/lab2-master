package com.example.lab1.model;

import java.time.Instant;

public record AudioFile(Long id, Long episodeId, String path, String format, Long sizeBytes, Boolean primary, Instant createdAt, Instant updatedAt) implements Resource {
}
