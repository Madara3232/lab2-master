package com.example.lab1.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Account(long userId, String email, String passwordHash, String role, boolean enabled) {}

    public Optional<Account> findByEmail(String email) {
        return jdbc.query("""
                SELECT a.user_id, u.email, a.password_hash, a.role, a.enabled
                FROM accounts a JOIN users u ON u.id = a.user_id
                WHERE lower(u.email) = lower(?)
                """, (row, index) -> new Account(row.getLong("user_id"), row.getString("email"),
                row.getString("password_hash"), row.getString("role"), row.getBoolean("enabled")), email)
                .stream().findFirst();
    }

    public boolean hasAdmin() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM accounts WHERE role = 'ADMIN')", Boolean.class));
    }

    public void create(long userId, String passwordHash, String role) {
        jdbc.update("INSERT INTO accounts (user_id, password_hash, role) VALUES (?, ?, ?)",
                userId, passwordHash, role);
    }

    public int updateAccess(long userId, String role, boolean enabled) {
        return jdbc.update("""
                UPDATE accounts SET role = ?, enabled = ? WHERE user_id = ? AND role <> 'ADMIN'
                """, role, enabled, userId);
    }
}
