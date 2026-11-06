package com.example.lab1.controller;

import com.example.lab1.service.AccountService;
import com.example.lab1.model.User;
import com.example.lab1.security.AccountPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    public record Registration(String name, String email, String password) {}
    public record Identity(long userId, String email, String role) {}
    public record CsrfResponse(String headerName, String token) {}

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<Identity> register(@RequestBody Registration request) {
        User user = accounts.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(201).body(new Identity(user.id(), user.email(), "READER"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Identity me(@AuthenticationPrincipal AccountPrincipal principal) {
        return new Identity(principal.getUserId(), principal.getUsername(), principal.getRole());
    }
}
