package com.example.lab1.repository;

import com.example.lab1.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends JdbcRepository<User> {
    public UserRepository(JdbcTemplate jdbc) {
        super(jdbc, (row, number) -> new User(row.getLong("id"),
                row.getString("name"), row.getString("email"),
                instant(row, "created_at"), instant(row, "updated_at")),
                "users", "name", "email");
    }

    public User save(Long id, String name, String email) {
        return super.save(id, name, email);
    }
}
