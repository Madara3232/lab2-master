package com.example.lab1.controller;

import com.example.lab1.model.User;
import com.example.lab1.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CatalogService service;

    public UserController(CatalogService service) {
        this.service = service;
    }

    public record Request(String name, String email) {}

    @GetMapping
    public List<User> list() {
        return service.listUsers();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody Request request) {
        User created = service.saveUser(null, request.name(), request.email());
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody Request request) {
        return service.saveUser(id, request.name(), request.email());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
