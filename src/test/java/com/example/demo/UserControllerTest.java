package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAll_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createAndGet_shouldWork() throws Exception {
        mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content("{\"name\":\"Alice\",\"email\":\"alice@test.com\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Alice"));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99")).andExpect(status().isNotFound());
    }

    @Test
    void update_shouldWork() throws Exception {
        mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content("{\"name\":\"Alice\",\"email\":\"alice@test.com\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/api/users/1")
            .contentType("application/json")
            .content("{\"name\":\"Bob\",\"email\":\"bob@test.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    void delete_shouldReturn204_whenFound() throws Exception {
        mockMvc.perform(post("/api/users")
            .contentType("application/json")
            .content("{\"name\":\"Alice\",\"email\":\"a@b.com\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/users/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/99")).andExpect(status().isNotFound());
    }
}
