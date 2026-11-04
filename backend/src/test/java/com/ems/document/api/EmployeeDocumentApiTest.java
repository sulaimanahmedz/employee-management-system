package com.ems.document.api;

import com.ems.document.entity.EmployeeDocument;
import com.ems.document.repository.EmployeeDocumentRepository;
import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;

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
        return employees.saveAndFlush(new Employee("Test", "Person", email,
            "Engineering", "Developer", EmployeeStatus.ACTIVE)).getId();
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
            "Original", EmployeeDocument.Category.CONTRACT, java.time.LocalDate.of(2027, 1, 1))).getId();
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
            "Original", EmployeeDocument.Category.CONTRACT, null)).getId();
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
    @Test void paginatesDocumentsWithoutChangingLegacyList() throws Exception {
        long owner = employee("paging@example.com"), other = employee("paging-other@example.com");
        for (int i = 0; i < 11; i++) documents.saveAndFlush(new EmployeeDocument(employees.findById(owner).orElseThrow(),
            "Document " + i, EmployeeDocument.Category.OTHER, null));
        mvc.perform(get(path(owner) + "/page")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(10)).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items[0].name").value("Document 10"));
        mvc.perform(get(path(owner) + "/page?page=1")).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Document 0"));
        mvc.perform(get(path(owner) + "/page?page=99")).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get(path(other) + "/page")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(owner))).andExpect(jsonPath("$.length()").value(11));
        for (String page : new String[] {"-1", "2147483647", "invalid"})
            mvc.perform(get(path(owner) + "/page?page=" + page)).andExpect(status().isBadRequest());
        mvc.perform(get(path(Long.MAX_VALUE) + "/page")).andExpect(status().isNotFound());
    }
    @Test void filtersExpiryInclusivelyBeforePaginationAndExcludesUndatedRecords() throws Exception {
        long owner = employee("expiry@example.com"), other = employee("expiry-other@example.com");
        var employee = employees.findById(owner).orElseThrow();
        for (int i = 0; i < 11; i++) documents.saveAndFlush(new EmployeeDocument(employee,
            "Due " + i, EmployeeDocument.Category.CERTIFICATE, java.time.LocalDate.of(2026, 12, 1)));
        documents.saveAndFlush(new EmployeeDocument(employee, "Undated", EmployeeDocument.Category.OTHER, null));
        documents.saveAndFlush(new EmployeeDocument(employee, "Later", EmployeeDocument.Category.OTHER, java.time.LocalDate.of(2026, 12, 2)));
        mvc.perform(get(path(owner) + "/page?expiresBy=2026-12-01")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(10)).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items[0].name").value("Due 10"));
        mvc.perform(get(path(owner) + "/page?expiresBy=2026-12-01&page=1")).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(path(owner) + "/page?expiresBy=2026-11-30")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(other) + "/page?expiresBy=2026-12-01")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(owner) + "/page")).andExpect(jsonPath("$.totalElements").value(13));
        mvc.perform(get(path(owner) + "/page?expiresBy=invalid")).andExpect(status().isBadRequest());
    }
    @Test void combinesCategoryAndExpiryFiltersAndPreservesUndatedCategoryMatches() throws Exception {
        long owner = employee("category@example.com"), other = employee("category-other@example.com");
        var employee = employees.findById(owner).orElseThrow();
        for (int i = 0; i < 11; i++) documents.saveAndFlush(new EmployeeDocument(employee,
            "Contract " + i, EmployeeDocument.Category.CONTRACT, java.time.LocalDate.of(2026, 12, 1)));
        documents.saveAndFlush(new EmployeeDocument(employee, "Undated", EmployeeDocument.Category.CONTRACT, null));
        documents.saveAndFlush(new EmployeeDocument(employee, "Certificate", EmployeeDocument.Category.CERTIFICATE, java.time.LocalDate.of(2026, 12, 1)));
        mvc.perform(get(path(owner) + "/page?category=CONTRACT")).andExpect(jsonPath("$.totalElements").value(12));
        mvc.perform(get(path(owner) + "/page?category=CONTRACT&expiresBy=2026-12-01"))
            .andExpect(jsonPath("$.items.length()").value(10)).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(path(owner) + "/page?category=CONTRACT&expiresBy=2026-12-01&page=1"))
            .andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(path(owner) + "/page?category=CERTIFICATE")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(path(other) + "/page?category=CONTRACT")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(owner) + "/page?category=INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get(path(owner) + "/page")).andExpect(jsonPath("$.totalElements").value(13));
    }

    @Test void searchesDocumentNamesBeforePaginationAndCombinesFilters() throws Exception {
        long owner = employee("search@example.com"), other = employee("search-other@example.com");
        var employee = employees.findById(owner).orElseThrow();
        for (int i = 0; i < 11; i++) documents.saveAndFlush(new EmployeeDocument(employee,
            "Safety certificate " + i, EmployeeDocument.Category.CERTIFICATE, java.time.LocalDate.of(2026, 12, 1)));
        documents.saveAndFlush(new EmployeeDocument(employee, "Safety contract", EmployeeDocument.Category.CONTRACT, null));
        documents.saveAndFlush(new EmployeeDocument(employees.findById(other).orElseThrow(),
            "Safety certificate", EmployeeDocument.Category.CERTIFICATE, java.time.LocalDate.of(2026, 12, 1)));
        mvc.perform(get(path(owner) + "/page").param("q", "  SAFETY  ").param("category", "CERTIFICATE").param("expiresBy", "2026-12-01"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items.length()").value(10));
        mvc.perform(get(path(owner) + "/page").param("q", "certificate").param("page", "1"))
            .andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(path(owner) + "/page").param("q", "contract"))
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(path(owner) + "/page").param("q", "contract").param("expiresBy", "2026-12-01"))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(owner) + "/page").param("q", "   "))
            .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test void treatsSearchWildcardsLiterallyAndValidatesLength() throws Exception {
        long owner = employee("literal@example.com");
        documents.saveAndFlush(new EmployeeDocument(employees.findById(owner).orElseThrow(),
            "100%_complete", EmployeeDocument.Category.OTHER, null));
        documents.saveAndFlush(new EmployeeDocument(employees.findById(owner).orElseThrow(),
            "Another record", EmployeeDocument.Category.OTHER, null));
        for (String term : new String[] {"%", "_", "100%_"}) {
            mvc.perform(get(path(owner) + "/page").param("q", term)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        }
        mvc.perform(get(path(owner) + "/page").param("q", "x".repeat(150))).andExpect(status().isOk());
        mvc.perform(get(path(owner) + "/page").param("q", "x".repeat(151))).andExpect(status().isBadRequest());
        mvc.perform(get(path(owner))).andExpect(jsonPath("$.length()").value(2));
    }
}
