package com.ems.employmenthistory;

import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.employmenthistory.repository.EmploymentHistoryRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
class EmploymentHistoryApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmployeeRepository employees;
    @Autowired EmploymentHistoryRepository history;
    @BeforeEach void clean() { employees.deleteAll(); }
    private String body(String email, String department, String title, String status) throws Exception {
        return json.writeValueAsString(Map.of("firstName", "Synthetic", "lastName", "Person", "email", email,
            "department", department, "jobTitle", title, "status", status));
    }
    private long create(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON)
            .content(body(email, "Engineering", "Developer", "ACTIVE"))).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }
    private void update(long id, String email, String department, String title, String status) throws Exception {
        mvc.perform(put("/api/v1/employees/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body(email, department, title, status))).andExpect(status().isOk());
    }
    @Test void recordsCreationAndOnlyEmploymentChangesWithStablePages() throws Exception {
        long id = create("history@example.com");
        update(id, "renamed@example.com", " Engineering ", " Developer ", "ACTIVE");
        mvc.perform(get("/api/v1/employees/" + id + "/history")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.items[0].kind").value("CREATED"));
        update(id, "renamed@example.com", "Operations", "Lead", "ON_LEAVE");
        mvc.perform(get("/api/v1/employees/" + id + "/history?size=1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.items[0].kind").value("CHANGED"))
            .andExpect(jsonPath("$.items[0].department").value("Operations"))
            .andExpect(jsonPath("$.items[0].jobTitle").value("Lead"))
            .andExpect(jsonPath("$.items[0].status").value("ON_LEAVE"))
            .andExpect(jsonPath("$.items[0].recordedAt").exists());
        mvc.perform(get("/api/v1/employees/" + id + "/history?size=1&page=1"))
            .andExpect(jsonPath("$.items[0].department").value("Engineering"));
        mvc.perform(get("/api/v1/employees/" + id + "/history?page=99"))
            .andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test void existingProfilesGetAnHonestBaselineAndHistoryIsScoped() throws Exception {
        long id = employees.saveAndFlush(new Employee("Synthetic", "Legacy", "legacy@example.com",
            "Finance", "Analyst", EmployeeStatus.ACTIVE)).getId();
        mvc.perform(get("/api/v1/employees/" + id + "/history")).andExpect(jsonPath("$.totalElements").value(0));
        update(id, "legacy@example.com", "Finance", "Senior Analyst", "ACTIVE");
        mvc.perform(get("/api/v1/employees/" + id + "/history"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.items[1].kind").value("BASELINE"))
            .andExpect(jsonPath("$.items[1].jobTitle").value("Analyst"));
        long other = create("other@example.com");
        mvc.perform(get("/api/v1/employees/" + other + "/history"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void rejectedUpdatesDoNotLeaveHistoryAndDeletionCleansItUp() throws Exception {
        create("taken@example.com");
        long id = create("original@example.com");
        mvc.perform(put("/api/v1/employees/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("taken@example.com", "Changed", "Lead", "INACTIVE"))).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/employees/" + id).contentType(MediaType.APPLICATION_JSON)
            .content(body("bad", "Changed", "Lead", "INACTIVE"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees/" + id + "/history"))
            .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/v1/employees/" + id)).andExpect(status().isNoContent());
        assertEquals(1, history.count());
        mvc.perform(get("/api/v1/employees/" + id + "/history")).andExpect(status().isNotFound());
    }
    @Test void rejectsInvalidPaginationAndMissingEmployees() throws Exception {
        long id = create("paging@example.com");
        for (String query : new String[] { "page=-1", "size=0", "size=101", "page=2147483647&size=100", "page=oops" }) {
            mvc.perform(get("/api/v1/employees/" + id + "/history?" + query)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/employees/" + Long.MAX_VALUE + "/history")).andExpect(status().isNotFound());
    }

    @Test void filtersEventKindsBeforePagingAndKeepsEmployeeScope() throws Exception {
        long id = create("kind@example.com"), other = create("kind-other@example.com");
        for (int i = 1; i <= 11; i++) update(id, "kind@example.com", "Engineering", "Role " + i, "ACTIVE");
        String path = "/api/v1/employees/" + id + "/history";
        mvc.perform(get(path + "?kind=CHANGED&size=10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items.length()").value(10))
            .andExpect(jsonPath("$.items[0].jobTitle").value("Role 11"));
        mvc.perform(get(path + "?kind=CHANGED&size=10&page=1"))
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].jobTitle").value("Role 1"));
        mvc.perform(get(path + "?kind=CREATED")).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].kind").value("CREATED"));
        mvc.perform(get(path + "?kind=BASELINE")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path)).andExpect(jsonPath("$.totalElements").value(12));
        mvc.perform(get("/api/v1/employees/" + other + "/history?kind=CHANGED"))
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path + "?kind=UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees/" + Long.MAX_VALUE + "/history?kind=CHANGED"))
            .andExpect(status().isNotFound());
    }

    @Test void filtersLegacyBaselineSeparatelyFromChanges() throws Exception {
        long id = employees.saveAndFlush(new Employee("Synthetic", "Legacy", "baseline-filter@example.com",
            "Finance", "Analyst", EmployeeStatus.ACTIVE)).getId();
        update(id, "baseline-filter@example.com", "Finance", "Senior Analyst", "ACTIVE");
        mvc.perform(get("/api/v1/employees/" + id + "/history?kind=BASELINE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].jobTitle").value("Analyst"));
        mvc.perform(get("/api/v1/employees/" + id + "/history?kind=CREATED"))
            .andExpect(jsonPath("$.totalElements").value(0));
    }
}
