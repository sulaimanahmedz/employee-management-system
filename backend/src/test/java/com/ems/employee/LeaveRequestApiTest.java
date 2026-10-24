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
class LeaveRequestApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EmployeeRepository employees;
    @Autowired LeaveRequestRepository requests;
    @BeforeEach void clean() { employees.deleteAll(); }
    private long employee(String email) {
        return employees.saveAndFlush(new Employee(new EmployeeRequest("Synthetic", "Person", email,
            "Engineering", "Developer", EmployeeStatus.ACTIVE))).getId();
    }
    private String path(long id) { return "/api/v1/employees/" + id + "/leave-requests"; }
    private String input(String start, String end) throws Exception {
        return json.writeValueAsString(Map.of("type", "ANNUAL", "startOn", start, "endOn", end));
    }
    private long add(long id, String start, String end) throws Exception {
        var body = mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON).content(input(start, end)))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("PENDING")).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
    @Test void recordsCalendarDaysAndCancelsIdempotentlyWithoutChangingEmployee() throws Exception {
        long id = employee("leave@example.com"), request = add(id, "2026-12-04", "2026-12-07");
        mvc.perform(get(path(id) + "/" + request)).andExpect(status().isOk()).andExpect(jsonPath("$.calendarDays").value(4));
        for (int i = 0; i < 2; i++) mvc.perform(put(path(id) + "/" + request + "/cancellation"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(1));
        assertEquals(EmployeeStatus.ACTIVE, employees.findById(id).orElseThrow().getStatus());
        add(id, "2026-12-04", "2026-12-07"); // cancelled dates can be requested again
    }
    @Test void rejectsInclusiveOverlapsButAllowsAdjacentDatesAndOtherEmployees() throws Exception {
        long id = employee("overlap@example.com"), other = employee("other@example.com");
        add(id, "2026-12-10", "2026-12-12");
        for (String[] dates : new String[][] {{"2026-12-10", "2026-12-12"}, {"2026-12-09", "2026-12-10"},
            {"2026-12-12", "2026-12-14"}, {"2026-12-11", "2026-12-11"}, {"2026-12-01", "2026-12-31"}})
            mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON).content(input(dates[0], dates[1])))
                .andExpect(status().isConflict());
        add(id, "2026-12-13", "2026-12-13");
        add(other, "2026-12-10", "2026-12-12");
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(2));
    }
    @Test void enforcesScopingAndDeletesWithEmployee() throws Exception {
        long id = employee("scope@example.com"), other = employee("scope-other@example.com");
        long request = add(id, "2026-12-01", "2026-12-01");
        mvc.perform(get(path(other) + "/" + request)).andExpect(status().isNotFound());
        mvc.perform(put(path(other) + "/" + request + "/cancellation")).andExpect(status().isNotFound());
        mvc.perform(get(path(other))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete("/api/v1/employees/" + id)).andExpect(status().isNoContent());
        assertEquals(0, requests.count());
        mvc.perform(get(path(id))).andExpect(status().isNotFound());
        mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON).content(input("2026-12-01", "2026-12-01")))
            .andExpect(status().isNotFound());
    }
    @Test void validatesInputAndPagination() throws Exception {
        long id = employee("validation@example.com");
        for (String body : new String[] {"{}", input("2026-12-02", "2026-12-01"), input("invalid", "2026-12-01"),
            json.writeValueAsString(Map.of("type", "UNKNOWN", "startOn", "2026-12-01", "endOn", "2026-12-01"))})
            mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        for (int day = 1; day <= 11; day++) add(id, "2026-12-%02d".formatted(day), "2026-12-%02d".formatted(day));
        mvc.perform(get(path(id))).andExpect(jsonPath("$.items.length()").value(10))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items[0].startOn").value("2026-12-11"));
        mvc.perform(get(path(id) + "?page=1")).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(path(id) + "?page=99")).andExpect(jsonPath("$.items.length()").value(0));
        for (String page : new String[] {"-1", "2147483647", "invalid"})
            mvc.perform(get(path(id) + "?page=" + page)).andExpect(status().isBadRequest());
        mvc.perform(put(path(id) + "/999999/cancellation")).andExpect(status().isNotFound());
    }
}
