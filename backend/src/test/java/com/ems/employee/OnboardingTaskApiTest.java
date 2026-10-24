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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OnboardingTaskApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmployeeRepository employees;
    @Autowired OnboardingTaskRepository tasks;
    @BeforeEach void clean() { employees.deleteAll(); }
    private long employee(String email) {
        return employees.saveAndFlush(new Employee(new EmployeeRequest("Synthetic", "Person", email,
            "Engineering", "Developer", EmployeeStatus.ACTIVE))).getId();
    }
    private String path(long id) { return "/api/v1/employees/" + id + "/onboarding-tasks"; }
    private long add(long id, String title) throws Exception {
        var body = mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", title, "owner", "  IT team  ", "dueOn", "2026-12-01"))))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.owner").value("IT team")).andExpect(jsonPath("$.completed").value(false))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
    @Test void lifecycleIsScopedAndCompletionIsIdempotent() throws Exception {
        long id = employee("owner@example.com"), other = employee("other@example.com");
        long task = add(id, "  Prepare laptop  ");
        mvc.perform(get(path(id) + "/" + task)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Prepare laptop"));
        mvc.perform(get(path(other) + "/" + task)).andExpect(status().isNotFound());
        mvc.perform(delete(path(other) + "/" + task)).andExpect(status().isNotFound());
        mvc.perform(put(path(other) + "/" + task + "/completion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"completed\":true}")).andExpect(status().isNotFound());
        for (int i = 0; i < 2; i++) mvc.perform(put(path(id) + "/" + task + "/completion")
            .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        mvc.perform(get(path(id))).andExpect(jsonPath("$.completedTasks").value(1)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(put(path(id) + "/" + task + "/completion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"completed\":false}")).andExpect(status().isOk());
        mvc.perform(get(path(id))).andExpect(jsonPath("$.completedTasks").value(0));
        mvc.perform(delete(path(id) + "/" + task)).andExpect(status().isNoContent());
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void paginationAndEmployeeDeletion() throws Exception {
        long id = employee("paging@example.com");
        for (int i = 0; i < 11; i++) add(id, "Task " + i);
        mvc.perform(get(path(id))).andExpect(jsonPath("$.items.length()").value(10))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items[0].title").value("Task 10"));
        mvc.perform(get(path(id) + "?page=1")).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("Task 0"));
        mvc.perform(get(path(id) + "?page=99")).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(delete("/api/v1/employees/" + id)).andExpect(status().isNoContent());
        assertEquals(0, tasks.count());
        mvc.perform(get(path(id))).andExpect(status().isNotFound());
    }
    @Test void validatesRequestsAndMissingRecords() throws Exception {
        long id = employee("validation@example.com");
        for (String body : new String[] { "{}", "{\"title\":\" \",\"owner\":\"IT\"}",
            "{\"title\":\"Task\",\"owner\":\" \"}", "{\"title\":\"Task\",\"owner\":\"IT\",\"dueOn\":\"invalid\"}",
            json.writeValueAsString(Map.of("title", "x".repeat(151), "owner", "IT")),
            json.writeValueAsString(Map.of("title", "Task", "owner", "x".repeat(101))) }) {
            mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        long task = add(id, "Check access");
        mvc.perform(put(path(id) + "/" + task + "/completion").contentType(MediaType.APPLICATION_JSON)
            .content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get(path(id) + "/999999")).andExpect(status().isNotFound());
        mvc.perform(get(path(id) + "?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get(path(id) + "?page=2147483647")).andExpect(status().isBadRequest());
        mvc.perform(get(path(Long.MAX_VALUE))).andExpect(status().isNotFound());
        mvc.perform(post(path(Long.MAX_VALUE)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Task\",\"owner\":\"IT\"}")).andExpect(status().isNotFound());
        mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Orientation\",\"owner\":\"HR\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.dueOn").isEmpty());
    }
    @Test void editsDetailsWithoutLosingCompletionAndCanClearDueDate() throws Exception {
        long id = employee("edit@example.com"), task = add(id, "Original");
        mvc.perform(put(path(id) + "/" + task + "/completion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"completed\":true}")).andExpect(status().isOk());
        mvc.perform(put(path(id) + "/" + task).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "  Updated task  ", "owner", "  HR team  "))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated task"))
            .andExpect(jsonPath("$.owner").value("HR team")).andExpect(jsonPath("$.dueOn").isEmpty())
            .andExpect(jsonPath("$.completed").value(true));
        mvc.perform(get(path(id) + "/" + task)).andExpect(jsonPath("$.title").value("Updated task"));
        mvc.perform(put(path(id) + "/" + task + "/completion").contentType(MediaType.APPLICATION_JSON)
            .content("{\"completed\":false}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated task")).andExpect(jsonPath("$.owner").value("HR team"));
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.completedTasks").value(0));
    }
    @Test void rejectedEditsLeaveTaskUnchanged() throws Exception {
        long id = employee("edit-owner@example.com"), other = employee("edit-other@example.com");
        long task = add(id, "Original");
        for (String body : new String[] {"{}", json.writeValueAsString(Map.of("title", " ", "owner", "HR")),
            "{\"title\":\"Changed\",\"owner\":\"HR\",\"dueOn\":\"invalid\"}"}) {
            mvc.perform(put(path(id) + "/" + task).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        String update = "{\"title\":\"Changed\",\"owner\":\"HR\"}";
        mvc.perform(put(path(other) + "/" + task).contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isNotFound());
        mvc.perform(put(path(id) + "/999999").contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isNotFound());
        mvc.perform(get(path(id) + "/" + task)).andExpect(jsonPath("$.title").value("Original"))
            .andExpect(jsonPath("$.owner").value("IT team")).andExpect(jsonPath("$.dueOn").value("2026-12-01"));
    }
    @Test void filtersBeforePaginationAndKeepsGlobalProgress() throws Exception {
        long id = employee("filters@example.com"), other = employee("unrelated@example.com");
        long completedTask = add(id, "Completed first");
        mvc.perform(put(path(id) + "/" + completedTask + "/completion").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("completed", true)))).andExpect(status().isOk());
        for (int i = 0; i < 11; i++) add(id, "Pending " + i);
        add(other, "Other employee");
        mvc.perform(get(path(id) + "?completed=false")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(10)).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.completedTasks").value(1))
            .andExpect(jsonPath("$.totalTasks").value(12));
        mvc.perform(get(path(id) + "?completed=false&page=1"))
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].title").value("Pending 0"));
        mvc.perform(get(path(id) + "?completed=true")).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].id").value(completedTask)).andExpect(jsonPath("$.totalTasks").value(12));
        mvc.perform(get(path(other) + "?completed=true")).andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalTasks").value(1));
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(12));
        mvc.perform(get(path(id) + "?completed=invalid")).andExpect(status().isBadRequest());
    }
}
