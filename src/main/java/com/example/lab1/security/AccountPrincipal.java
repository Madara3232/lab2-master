package com.example.lab1.security;

import com.example.lab1.repository.AccountRepository.Account;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class AccountPrincipal extends User {
    private final long userId;
    private final String role;

    public AccountPrincipal(Account account) {
        super(account.email(), account.passwordHash(), account.enabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role())));
        userId = account.userId();
        role = account.role();
    }

    public long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
