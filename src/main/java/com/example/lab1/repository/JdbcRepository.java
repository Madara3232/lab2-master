package com.example.lab1.repository;

import com.example.lab1.model.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Table and column names are fixed by the concrete repositories. Values are bound as parameters. */
public abstract class JdbcRepository<T extends Resource> {
    protected final JdbcTemplate jdbc;
    private final RowMapper<T> mapper;
    private final String table;
    private final String insert;
    private final String update;

    protected JdbcRepository(JdbcTemplate jdbc, RowMapper<T> mapper, String table, String... columns) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.table = table;
        String placeholders = Arrays.stream(columns).map(column -> "?").collect(Collectors.joining(", "));
        String assignments = Arrays.stream(columns).map(column -> column + " = ?").collect(Collectors.joining(", "));
        insert = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ") RETURNING *";
        update = "UPDATE " + table + " SET " + assignments + ", updated_at = clock_timestamp() WHERE id = ? RETURNING *";
    }

    public List<T> findAll() {
        return jdbc.query("SELECT * FROM " + table + " ORDER BY id", mapper);
    }

    public Optional<T> findById(Long id) {
        return findOne("id = ?", id);
    }

    protected Optional<T> findOne(String condition, Object... values) {
        return jdbc.query("SELECT * FROM " + table + " WHERE " + condition, mapper, values).stream().findFirst();
    }

    protected T save(Long id, Object... values) {
        if (id == null) return jdbc.queryForObject(insert, mapper, values);
        Object[] parameters = Arrays.copyOf(values, values.length + 1);
        parameters[values.length] = id;
        return jdbc.queryForObject(update, mapper, parameters);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM " + table + " WHERE id = ?", id);
    }

    protected static Instant instant(ResultSet row, String column) throws SQLException {
        return offset(row, column).toInstant();
    }

    protected static OffsetDateTime offset(ResultSet row, String column) throws SQLException {
        return row.getObject(column, OffsetDateTime.class);
    }
}
