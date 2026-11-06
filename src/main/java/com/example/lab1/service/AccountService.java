package com.example.lab1.service;

import com.example.lab1.model.User;
import com.example.lab1.repository.AccountRepository;
import com.example.lab1.repository.CatalogWriteLock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static com.example.lab1.service.CatalogService.bad;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AccountService {
    private final CatalogService catalog;
    private final AccountRepository accounts;
    private final CatalogWriteLock lock;
    private final PasswordEncoder encoder;

    public AccountService(CatalogService catalog, AccountRepository accounts, CatalogWriteLock lock,
                          PasswordEncoder encoder) {
        this.catalog = catalog;
        this.accounts = accounts;
        this.lock = lock;
        this.encoder = encoder;
    }

    public User register(String name, String email, String password) {
        String hash = encodeStrongPassword(password);
        User profile = catalog.saveUser(null, name, email);
        accounts.create(profile.id(), hash, "READER");
        return profile;
    }

    public void updateAccess(Long id, String role, Boolean enabled) {
        if (!("READER".equals(role) || "EDITOR".equals(role)) || enabled == null) {
            throw bad("role must be READER or EDITOR and enabled must be provided");
        }
        lock.acquire();
        catalog.getUser(id);
        if (accounts.updateAccess(id, role, enabled) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Manageable account not found");
        }
    }

    public void bootstrapAdmin(String email, String password) {
        if (email.isBlank() && password.isEmpty()) return;
        if (email.isBlank() || password.isEmpty()) {
            throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD for initial setup");
        }
        lock.acquire();
        if (accounts.hasAdmin()) return;
        String hash = encodeStrongPassword(password);
        User profile = catalog.saveUser(null, "Administrator", email);
        accounts.create(profile.id(), hash, "ADMIN");
    }

    private String encodeStrongPassword(String password) {
        if (password == null || password.length() > 72
                || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.indexOf(0) >= 0
                || password.codePoints().noneMatch(Character::isUpperCase)
                || password.codePoints().noneMatch(Character::isLowerCase)
                || password.codePoints().noneMatch(Character::isDigit)
                || password.codePoints().noneMatch(c -> !Character.isLetterOrDigit(c)
                        && !Character.isWhitespace(c) && !Character.isISOControl(c))) {
            throw bad("Password needs at least 12 characters, uppercase, lowercase, digit and symbol, and at most 72 UTF-8 bytes");
        }
        return encoder.encode(password);
    }
}
