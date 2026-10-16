package com.example.lab1.model;

import java.time.Instant;

public record PublicationSlot(Long id, Long podcastId, Long episodeId, java.time.OffsetDateTime scheduledAt, String status, Instant createdAt, Instant updatedAt) implements Resource {
}
