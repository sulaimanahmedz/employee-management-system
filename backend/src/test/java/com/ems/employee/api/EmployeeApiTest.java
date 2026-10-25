package com.ems.employee.api;

import com.ems.employee.repository.EmployeeRepository;

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

    private long createProfile(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON)
            .content(request(email))).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test void savesAndRetrievesAllProfileFields() throws Exception {
        long id = createProfile("original@example.com");
        String update = json.writeValueAsString(Map.of(
            "firstName", "  Noor  ", "lastName", "  Ali  ", "email", "NOOR@example.com",
            "department", "  Finance  ", "jobTitle", "  Analyst  ", "status", "ON_LEAVE"));
        mvc.perform(put("/api/v1/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(update)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/v1/employees/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Noor"))
            .andExpect(jsonPath("$.lastName").value("Ali"))
            .andExpect(jsonPath("$.email").value("noor@example.com"))
            .andExpect(jsonPath("$.department").value("Finance"))
            .andExpect(jsonPath("$.jobTitle").value("Analyst"))
            .andExpect(jsonPath("$.status").value("ON_LEAVE"));
        mvc.perform(get("/api/v1/employees")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].department").value("Finance"));
    }

    @Test void invalidProfileUpdatePreservesStoredDetails() throws Exception {
        long id = createProfile("original@example.com");
        mvc.perform(put("/api/v1/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(request("invalid"))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(get("/api/v1/employees/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("original@example.com"));
    }

    @Test void duplicateProfileUpdateRollsBackAllChanges() throws Exception {
        createProfile("taken@example.com");
        long id = createProfile("original@example.com");
        String update = json.writeValueAsString(Map.of(
            "firstName", "Changed", "lastName", "Name", "email", "TAKEN@example.com",
            "department", "Finance", "jobTitle", "Analyst", "status", "INACTIVE"));
        mvc.perform(put("/api/v1/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(update)).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/employees/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Aisha"))
            .andExpect(jsonPath("$.email").value("original@example.com"))
            .andExpect(jsonPath("$.department").value("Engineering"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test void profileCanRetainItsOwnEmail() throws Exception {
        long id = createProfile("original@example.com");
        mvc.perform(put("/api/v1/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(request("ORIGINAL@example.com"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("original@example.com"));
    }

    @Test void deletedProfileCannotBeUpdated() throws Exception {
        long id = createProfile("original@example.com");
        mvc.perform(delete("/api/v1/employees/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(request("updated@example.com"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/employees")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
