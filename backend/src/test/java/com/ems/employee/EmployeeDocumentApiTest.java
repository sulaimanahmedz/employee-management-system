package com.ems.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeDocumentApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmployeeRepository employees;
    @Autowired EmployeeDocumentRepository documents;
    @BeforeEach void clean() { documents.deleteAll(); employees.deleteAll(); }
    private long employee(String email) {
        return employees.saveAndFlush(new Employee(new EmployeeRequest("Test", "Person", email,
            "Engineering", "Developer", EmployeeStatus.ACTIVE))).getId();
    }
    private String path(long employee) { return "/api/v1/employees/" + employee + "/documents"; }
    @Test void metadataLifecycleAndEmployeeScoping() throws Exception {
        long owner = employee("owner@example.com"), other = employee("other@example.com");
        mvc.perform(get(path(owner))).andExpect(status().isOk()).andExpect(content().json("[]"));
        String body = mvc.perform(post(path(owner)).contentType(MediaType.APPLICATION_JSON).content(
            "{\"name\":\"  Safety certificate  \",\"category\":\"CERTIFICATE\",\"expiresOn\":\"2027-12-31\"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Safety certificate"))
            .andExpect(jsonPath("$.expiresOn").value("2027-12-31"))
            .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();
        mvc.perform(get(path(owner))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(path(other))).andExpect(content().json("[]"));
        mvc.perform(get(path(owner) + "/" + id)).andExpect(status().isOk());
        mvc.perform(get(path(other) + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete(path(other) + "/" + id)).andExpect(status().isNotFound());
        assertEquals(1, documents.count());
        mvc.perform(delete(path(owner) + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete(path(owner) + "/" + id)).andExpect(status().isNotFound());
        assertEquals(0, documents.count());
    }
    @Test void validatesMetadataAndMissingEmployees() throws Exception {
        long owner = employee("owner@example.com");
        for (String body : new String[] {
            "{\"name\":\" \",\"category\":\"OTHER\"}",
            "{\"name\":\"Certificate\"}",
            "{\"name\":\"Certificate\",\"category\":\"INVALID\"}",
            "{\"name\":\"Certificate\",\"category\":\"OTHER\",\"expiresOn\":\"invalid\"}",
            "{\"name\":\"" + "x".repeat(151) + "\",\"category\":\"OTHER\"}"
        }) mvc.perform(post(path(owner)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        mvc.perform(get(path(Long.MAX_VALUE))).andExpect(status().isNotFound());
        mvc.perform(post(path(Long.MAX_VALUE)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Contract\",\"category\":\"CONTRACT\"}"))
            .andExpect(status().isNotFound());
        assertEquals(0, documents.count());
    }
    @Test void optionalExpiryAndEmployeeDeletionCleanup() throws Exception {
        long owner = employee("owner@example.com");
        mvc.perform(post(path(owner)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Contract\",\"category\":\"CONTRACT\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.expiresOn").isEmpty());
        mvc.perform(delete("/api/v1/employees/" + owner)).andExpect(status().isNoContent());
        assertEquals(0, documents.count());
    }
    @Test void editsMetadataAndClearsExpiryWithoutCreatingAnotherRecord() throws Exception {
        long owner = employee("edit@example.com");
        long id = documents.saveAndFlush(new EmployeeDocument(employees.findById(owner).orElseThrow(),
            new DocumentRequest("Original", EmployeeDocument.Category.CONTRACT, java.time.LocalDate.of(2027, 1, 1)))).getId();
        String update = json.writeValueAsString(java.util.Map.of("name", "  Renewed certificate  ", "category", "CERTIFICATE"));
        mvc.perform(put(path(owner) + "/" + id).contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Renewed certificate"))
            .andExpect(jsonPath("$.category").value("CERTIFICATE")).andExpect(jsonPath("$.expiresOn").isEmpty());
        mvc.perform(get(path(owner) + "/" + id)).andExpect(jsonPath("$.name").value("Renewed certificate"));
        assertEquals(1, documents.count());
    }
    @Test void rejectsInvalidAndCrossEmployeeEditsWithoutChangingMetadata() throws Exception {
        long owner = employee("edit-owner@example.com"), other = employee("edit-other@example.com");
        long id = documents.saveAndFlush(new EmployeeDocument(employees.findById(owner).orElseThrow(),
            new DocumentRequest("Original", EmployeeDocument.Category.CONTRACT, null))).getId();
        for (String body : new String[] {"{}", json.writeValueAsString(java.util.Map.of("name", " ", "category", "OTHER")),
            json.writeValueAsString(java.util.Map.of("name", "x".repeat(151), "category", "OTHER")),
            json.writeValueAsString(java.util.Map.of("name", "Changed", "category", "INVALID")),
            json.writeValueAsString(java.util.Map.of("name", "Changed", "category", "OTHER", "expiresOn", "invalid"))})
            mvc.perform(put(path(owner) + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        String update = json.writeValueAsString(java.util.Map.of("name", "Changed", "category", "OTHER"));
        for (String url : new String[] {path(other) + "/" + id, path(owner) + "/999999", path(Long.MAX_VALUE) + "/" + id})
            mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isNotFound());
        mvc.perform(get(path(owner) + "/" + id)).andExpect(jsonPath("$.name").value("Original"))
            .andExpect(jsonPath("$.category").value("CONTRACT"));
        assertEquals(1, documents.count());
    }
}
