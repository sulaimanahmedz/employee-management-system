package com.ems.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmployeeRepository repository;
    @BeforeEach void clean() { repository.deleteAll(); }
    private String request(String email) throws Exception {
        return json.writeValueAsString(Map.of("firstName", "Aisha", "lastName", "Khan", "email", email,
            "department", "Engineering", "jobTitle", "Developer", "status", "ACTIVE"));
    }
    @Test void employeeLifecycle() throws Exception {
        String body = mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON)
            .content(request("AISHA@example.com")))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("aisha@example.com"))
            .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();
        mvc.perform(get("/api/v1/employees")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/employees/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(put("/api/v1/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(request("updated@example.com"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("updated@example.com"));
        mvc.perform(delete("/api/v1/employees/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/employees/{id}", id)).andExpect(status().isNotFound());
    }
    @Test void rejectsInvalidDetails() throws Exception {
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(request("invalid")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
    }
    @Test void rejectsDuplicateEmailIgnoringCase() throws Exception {
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(request("aisha@example.com")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(request("AISHA@example.com")))
            .andExpect(status().isConflict());
    }
    @Test void healthEndpointIsAvailable() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
