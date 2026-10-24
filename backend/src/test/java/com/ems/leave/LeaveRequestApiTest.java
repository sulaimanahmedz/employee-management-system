package com.ems.leave;

import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;
import com.ems.leave.repository.LeaveRequestRepository;

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
        return employees.saveAndFlush(new Employee("Synthetic", "Person", email,
            "Engineering", "Developer", EmployeeStatus.ACTIVE)).getId();
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
    @Test void filtersByStatusAndInclusiveOverlappingPeriodBeforePagination() throws Exception {
        long id = employee("filter@example.com"), other = employee("filter-other@example.com");
        long cancelled = add(id, "2026-11-28", "2026-12-03");
        mvc.perform(put(path(id) + "/" + cancelled + "/cancellation")).andExpect(status().isOk());
        for (int day = 1; day <= 11; day++) add(id, "2026-12-%02d".formatted(day), "2026-12-%02d".formatted(day));
        add(other, "2026-12-01", "2026-12-01");
        mvc.perform(get(path(id) + "?status=PENDING")).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items.length()").value(10));
        mvc.perform(get(path(id) + "?status=PENDING&page=1")).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(path(id) + "?status=CANCELLED&from=2026-12-03&to=2026-12-03"))
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.items[0].id").value(cancelled));
        mvc.perform(get(path(id) + "?from=2026-12-03&to=2026-12-03")).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get(path(id) + "?from=2026-12-11")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(path(id) + "?to=2026-11-28")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(path(id) + "?status=CANCELLED&from=2026-12-04")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(other) + "?status=CANCELLED")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(12));
        for (String query : new String[] {"status=UNKNOWN", "from=invalid", "to=invalid", "from=2026-12-02&to=2026-12-01"})
            mvc.perform(get(path(id) + "?" + query)).andExpect(status().isBadRequest());
    }
    @Test void combinesLeaveTypeWithStatusAndPeriodBeforePagination() throws Exception {
        long id = employee("type@example.com"), other = employee("type-other@example.com");
        for (int day = 1; day <= 11; day++) add(id, "2026-12-%02d".formatted(day), "2026-12-%02d".formatted(day));
        mvc.perform(post(path(id)).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("type", "UNPAID", "startOn", "2026-12-12", "endOn", "2026-12-12"))))
            .andExpect(status().isCreated());
        long cancelled = add(id, "2026-12-13", "2026-12-13");
        mvc.perform(put(path(id) + "/" + cancelled + "/cancellation")).andExpect(status().isOk());
        mvc.perform(get(path(id) + "?type=ANNUAL&status=PENDING&from=2026-12-01&to=2026-12-31"))
            .andExpect(jsonPath("$.totalElements").value(11)).andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.items.length()").value(10));
        mvc.perform(get(path(id) + "?type=ANNUAL&status=PENDING&page=1")).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(path(id) + "?type=ANNUAL&status=CANCELLED")).andExpect(jsonPath("$.items[0].id").value(cancelled));
        mvc.perform(get(path(id) + "?type=UNPAID")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(path(id) + "?type=PERSONAL")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(other) + "?type=ANNUAL")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(id) + "?type=INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get(path(id))).andExpect(jsonPath("$.totalElements").value(13));
    }

    @Test void editsPendingDetailsWithoutSelfOverlapAndRefreshesFilters() throws Exception {
        long id = employee("edit@example.com"), request = add(id, "2026-12-01", "2026-12-03");
        String url = path(id) + "/" + request;
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(input("2026-12-01", "2026-12-03")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(request));
        String changed = json.writeValueAsString(Map.of("type", "UNPAID", "startOn", "2026-12-02", "endOn", "2026-12-05"));
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(changed))
            .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("UNPAID"))
            .andExpect(jsonPath("$.calendarDays").value(4)).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(get(url)).andExpect(jsonPath("$.startOn").value("2026-12-02"))
            .andExpect(jsonPath("$.endOn").value("2026-12-05"));
        mvc.perform(get(path(id) + "?type=ANNUAL")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get(path(id) + "?type=UNPAID&from=2026-12-05")).andExpect(jsonPath("$.totalElements").value(1));
        add(id, "2026-12-01", "2026-12-01"); // moving dates releases the old day
        assertEquals(EmployeeStatus.ACTIVE, employees.findById(id).orElseThrow().getStatus());
    }

    @Test void rejectsEditOverlapsAndCancelledEditsWithoutChangingStoredDetails() throws Exception {
        long id = employee("edit-conflict@example.com"), other = employee("edit-other@example.com");
        long request = add(id, "2026-12-01", "2026-12-02");
        long blocker = add(id, "2026-12-10", "2026-12-12");
        String url = path(id) + "/" + request;
        for (String[] dates : new String[][] {{"2026-12-09", "2026-12-10"}, {"2026-12-12", "2026-12-14"},
                {"2026-12-11", "2026-12-11"}, {"2026-12-01", "2026-12-31"}}) {
            mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(input(dates[0], dates[1])))
                .andExpect(status().isConflict());
        }
        mvc.perform(get(url)).andExpect(jsonPath("$.startOn").value("2026-12-01"))
            .andExpect(jsonPath("$.endOn").value("2026-12-02"));
        add(other, "2026-12-13", "2026-12-13");
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(input("2026-12-13", "2026-12-13")))
            .andExpect(status().isOk()); // adjacent dates and another employee do not conflict
        mvc.perform(put(path(id) + "/" + blocker + "/cancellation")).andExpect(status().isOk());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(input("2026-12-10", "2026-12-12")))
            .andExpect(status().isOk()); // cancelled records do not block edits
        mvc.perform(put(url + "/cancellation")).andExpect(status().isOk());
        mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(input("2026-12-20", "2026-12-20")))
            .andExpect(status().isConflict());
        mvc.perform(get(url)).andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.startOn").value("2026-12-10"));
    }

    @Test void validatesEditInputAndEmployeeScope() throws Exception {
        long id = employee("edit-validation@example.com"), other = employee("edit-scope@example.com");
        long request = add(id, "2026-12-01", "2026-12-01");
        String url = path(id) + "/" + request;
        for (String body : new String[] {"{}", input("2026-12-02", "2026-12-01"), input("invalid", "2026-12-01"),
                json.writeValueAsString(Map.of("type", "UNKNOWN", "startOn", "2026-12-01", "endOn", "2026-12-01"))}) {
            mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        for (String missing : new String[] {path(other) + "/" + request, path(id) + "/999999", path(999999L) + "/" + request}) {
            mvc.perform(put(missing).contentType(MediaType.APPLICATION_JSON).content(input("2026-12-02", "2026-12-02")))
                .andExpect(status().isNotFound());
        }
        mvc.perform(get(url)).andExpect(jsonPath("$.startOn").value("2026-12-01"));
    }
}
