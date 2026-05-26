package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void findAll_shouldReturnEmptyInitially() {
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void create_shouldReturnUserWithId() {
        User user = service.create("Alice", "alice@test.com");
        assertEquals(1L, user.id());
        assertEquals("Alice", user.name());
    }

    @Test
    void findById_shouldReturnUser_whenExists() {
        service.create("Alice", "alice@test.com");
        var opt = service.findById(1L);
        assertTrue(opt.isPresent());
        assertEquals("Alice", opt.get().name());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        assertTrue(service.findById(99L).isEmpty());
    }

    @Test
    void update_shouldModifyUser_whenExists() {
        service.create("Alice", "alice@test.com");
        var opt = service.update(1L, "Bob", "bob@test.com");
        assertTrue(opt.isPresent());
        assertEquals("Bob", opt.get().name());
        assertEquals("Bob", service.findById(1L).get().name());
    }

    @Test
    void update_shouldReturnEmpty_whenNotFound() {
        assertTrue(service.update(99L, "Bob", "bob@test.com").isEmpty());
    }

    @Test
    void delete_shouldRemoveUser_whenExists() {
        service.create("Alice", "alice@test.com");
        assertTrue(service.delete(1L));
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void delete_shouldReturnFalse_whenNotFound() {
        assertFalse(service.delete(99L));
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        service.create("Alice", "alice@test.com");
        service.create("Bob", "bob@test.com");
        List<User> users = service.findAll();
        assertEquals(2, users.size());
    }
}
