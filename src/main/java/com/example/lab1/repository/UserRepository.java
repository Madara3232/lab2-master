package com.example.lab1.repository;

import com.example.lab1.model.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends InMemoryRepository<User> {
}
