package com.example.demo;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private final List<User> users = new ArrayList<>();
    private final AtomicLong counter = new AtomicLong();

    public List<User> findAll() {
        return users;
    }

    public Optional<User> findById(Long id) {
        return users.stream().filter(u -> u.id().equals(id)).findFirst();
    }

    public User create(String name, String email) {
        User user = new User(counter.incrementAndGet(), name, email);
        users.add(user);
        return user;
    }

    public Optional<User> update(Long id, String name, String email) {
        return findById(id).map(u -> {
            users.remove(u);
            User updated = new User(id, name, email);
            users.add(updated);
            return updated;
        });
    }

    public boolean delete(Long id) {
        return users.removeIf(u -> u.id().equals(id));
    }
}
