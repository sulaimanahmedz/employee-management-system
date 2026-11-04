package com.ems.employee.api;

import com.ems.employee.entity.Employee;
import com.ems.employee.entity.EmployeeStatus;
import com.ems.employee.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeDirectoryApiTest {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository repository;
    private long firstId;
    private long lastId;

    @BeforeEach void seedSyntheticEmployees() {
        repository.deleteAll();
        firstId = repository.saveAndFlush(new Employee(
            "Aisha", "Khan", "aisha@example.com", "Engineering", "Developer", EmployeeStatus.ACTIVE)).getId();
        repository.saveAndFlush(new Employee(
            "Noor", "Ali", "noor@example.com", "Finance", "Analyst", EmployeeStatus.ON_LEAVE));
        lastId = repository.saveAndFlush(new Employee(
            "Zoya", "Ahmed", "zoya@example.com", "Engineering", "Lead", EmployeeStatus.INACTIVE)).getId();
    }

    @Test void returnsStablePagesAndGlobalTotals() throws Exception {
        mvc.perform(get("/api/v1/employees/directory").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(firstId))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.totalEmployees").value(3))
            .andExpect(jsonPath("$.activeEmployees").value(1))
            .andExpect(jsonPath("$.departments").value(2));
        mvc.perform(get("/api/v1/employees/directory").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(lastId));
    }

    @Test void searchesAllRecordsNotJustFirstPage() throws Exception {
        mvc.perform(get("/api/v1/employees/directory").param("size", "1").param("q", "  ZOYA AHMED  "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(lastId))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalEmployees").value(3));
        for (String term : new String[] {"zoya@", "Lead"}) {
            mvc.perform(get("/api/v1/employees/directory").param("q", term))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(lastId))
                .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Test void combinesSearchAndStatusWithoutFilteringOverview() throws Exception {
        mvc.perform(get("/api/v1/employees/directory").param("q", "engineering").param("status", "INACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(lastId))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.activeEmployees").value(1))
            .andExpect(jsonPath("$.departments").value(2));
    }

    @Test void treatsSearchWildcardsAsLiteralText() throws Exception {
        for (String term : new String[] {"%", "_"}) {
            mvc.perform(get("/api/v1/employees/directory").param("q", term))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
        }
    }

    @Test void returnsEmptyOutOfRangePageWithCorrectCount() throws Exception {
        mvc.perform(get("/api/v1/employees/directory").param("page", "9").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.page").value(9))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test void rejectsInvalidParameters() throws Exception {
        for (String size : new String[] {"0", "-1", "101", "text"}) {
            mvc.perform(get("/api/v1/employees/directory").param("size", size))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/employees/directory").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees/directory").param("page", "2147483647").param("size", "100"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees/directory").param("q", "x".repeat(201)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees/directory").param("status", "UNKNOWN"))
            .andExpect(status().isBadRequest());
    }

    @Test void handlesEmptyDirectoryAndPreservesLegacyList() throws Exception {
        mvc.perform(get("/api/v1/employees"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        repository.deleteAll();
        mvc.perform(get("/api/v1/employees/directory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.totalEmployees").value(0))
            .andExpect(jsonPath("$.activeEmployees").value(0))
            .andExpect(jsonPath("$.departments").value(0));
    }

    @Test void sortsNamesBeforePagingInBothDirections() throws Exception {
        mvc.perform(get("/api/v1/employees/directory").param("sortBy", "name").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(lastId));
        mvc.perform(get("/api/v1/employees/directory").param("sortBy", "name").param("direction", "desc").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(firstId));
        mvc.perform(get("/api/v1/employees/directory").param("sortBy", "name").param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].lastName").value("Ali"))
            .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalEmployees").value(3));
        mvc.perform(get("/api/v1/employees/directory").param("direction", "desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(lastId));
    }

    @Test void sortsCaseInsensitivelyWithStableIdTiesAndCombinedFilters() throws Exception {
        long duplicate = repository.saveAndFlush(new Employee("aisha", "khan", "duplicate@example.com",
            "engineering", "developer", EmployeeStatus.ACTIVE)).getId();
        for (String direction : new String[] {"asc", "desc"}) {
            mvc.perform(get("/api/v1/employees/directory").param("sortBy", "name").param("direction", direction)
                    .param("q", "aisha").param("status", "ACTIVE").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(firstId))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalEmployees").value(4));
            mvc.perform(get("/api/v1/employees/directory").param("sortBy", "name").param("direction", direction)
                    .param("q", "aisha").param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.items[0].id").value(duplicate));
        }
        mvc.perform(get("/api/v1/employees/directory").param("sortBy", "department").param("direction", "desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].department").value("Finance"))
            .andExpect(jsonPath("$.items[1].id").value(firstId)).andExpect(jsonPath("$.items[2].id").value(lastId))
            .andExpect(jsonPath("$.items[3].id").value(duplicate));
        mvc.perform(get("/api/v1/employees/directory").param("sortBy", "jobTitle"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].jobTitle").value("Analyst"))
            .andExpect(jsonPath("$.items[1].id").value(firstId)).andExpect(jsonPath("$.items[2].id").value(duplicate));
    }

    @Test void rejectsUnsupportedSortsAndHandlesEmptySortedResults() throws Exception {
        for (String sort : new String[] {"salary", "employee.id", "name,desc", "lastName"}) {
            mvc.perform(get("/api/v1/employees/directory").param("sortBy", sort)).andExpect(status().isBadRequest());
        }
        for (String direction : new String[] {"up", "DESC", "asc,desc"}) {
            mvc.perform(get("/api/v1/employees/directory").param("direction", direction)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/employees/directory").param("sortBy", "department").param("q", "no-match"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.totalEmployees").value(3));
    }
}
