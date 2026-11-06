package com.example.lab1.controller;

import com.example.lab1.model.User;
import com.example.lab1.service.CatalogService;
import com.example.lab1.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/users")
public class UserController {
    private final CatalogService service;

    private final AccountService accounts;

    public UserController(CatalogService service, AccountService accounts) {
        this.accounts = accounts;
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
    @PreAuthorize("hasRole('ADMIN') and #id != principal.userId")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    public record AccountAccessRequest(String role, Boolean enabled) {}

    @PatchMapping("/{id}/access")
    @PreAuthorize("hasRole('ADMIN') and #id != principal.userId")
    public ResponseEntity<Void> updateAccess(@PathVariable Long id, @RequestBody AccountAccessRequest request) {
        accounts.updateAccess(id, request.role(), request.enabled());
        return ResponseEntity.noContent().build();
    }
}
