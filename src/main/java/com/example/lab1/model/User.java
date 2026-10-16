package com.example.lab1.model;

import java.time.Instant;

public record User(Long id, String name, String email, Instant createdAt, Instant updatedAt) implements Resource {
}
