package com.example.lab1.model;

import java.time.Instant;

public record Episode(Long id, Long podcastId, String title, Integer durationSeconds, String publicationState, Instant createdAt, Instant updatedAt) implements Resource {
}
