package com.example.lab1.model;

import java.time.Instant;

public record Podcast(Long id, String title, Long authorId, String status, Instant createdAt, Instant updatedAt) implements Resource {
}
