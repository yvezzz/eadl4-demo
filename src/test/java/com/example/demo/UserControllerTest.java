package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService service;

    @Test
    void getAll_shouldReturnList() throws Exception {
        when(service.findAll()).thenReturn(List.of(new User(1L, "Alice", "alice@test.com")));
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void getById_shouldReturnUser_whenFound() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(new User(1L, "Bob", "bob@test.com")));
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/users/99")).andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn201() throws Exception {
        when(service.create("Alice", "alice@test.com")).thenReturn(new User(1L, "Alice", "alice@test.com"));
        mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content("{\"name\":\"Alice\",\"email\":\"alice@test.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void update_shouldReturn200_whenFound() throws Exception {
        when(service.update(1L, "Alice", "a@b.com")).thenReturn(Optional.of(new User(1L, "Alice", "a@b.com")));
        mockMvc.perform(put("/api/users/1")
            .contentType("application/json")
            .content("{\"name\":\"Alice\",\"email\":\"a@b.com\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void delete_shouldReturn204_whenFound() throws Exception {
        when(service.delete(1L)).thenReturn(true);
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        when(service.delete(99L)).thenReturn(false);
        mockMvc.perform(delete("/api/users/99")).andExpect(status().isNotFound());
    }
}
