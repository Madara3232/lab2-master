package com.example.lab1.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogWriteLock {
    private final JdbcTemplate jdbc;

    public CatalogWriteLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every API mutation acquires this lock before reading state in its transaction. */
    public void acquire() {
        jdbc.query("SELECT pg_advisory_xact_lock(24021, 3)", row -> {});
    }
}
