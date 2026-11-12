package com.ems.identity.api;

import com.ems.identity.entity.AccountRole;
import com.ems.identity.repository.AccountRepository;
import com.ems.identity.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:identity-api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SessionApiTest {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired AccountRepository repository;
    @Autowired ObjectMapper json;
    private static final String PASSWORD = "Synthetic integration password!";
    @BeforeEach void seed() {
        repository.deleteAll();
        for (AccountRole role : AccountRole.values()) accounts.provision("test-" + role.name().toLowerCase(), PASSWORD, role);
    }
    @Test void loginLimitAppliesAcrossSessionsWithoutRevealingAccountExistence() throws Exception {
        accounts.provision("synthetic-limited", PASSWORD, AccountRole.HR);
        for (String name : new String[]{"synthetic-limited", "synthetic-absent"}) {
            for (int i = 0; i < 5; i++) mvc.perform(post("/api/v1/auth/login").with(csrf())
                .param("username", name).param("password", "wrong")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/auth/login").with(csrf()).session(new MockHttpSession())
                .param("username", " " + name.toUpperCase() + " ").param("password", PASSWORD))
                .andExpect(status().isUnauthorized()).andExpect(content().string(""));
        }
    }
    private MockHttpSession login(String name) throws Exception {
        var tokenResult = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(tokenResult.getResponse().getContentAsString());
        var session = (MockHttpSession) tokenResult.getRequest().getSession(false);
        String oldId = session.getId();
        mvc.perform(post("/api/v1/auth/login").session(session)
            .header(token.get("headerName").asText(), token.get("token").asText())
            .param("username", name).param("password", PASSWORD)).andExpect(status().isNoContent());
        assertNotEquals(oldId, session.getId(), "Login must rotate the session ID");
        return session;
    }
    @Test void sessionsRequireCredentialsAndCsrfAndLogoutInvalidatesThem() throws Exception {
        mvc.perform(get("/api/v1/employees")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/session")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").param("username", "test-hr").param("password", PASSWORD)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "missing").param("password", PASSWORD)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username", "test-hr").param("password", "incorrect")).andExpect(status().isUnauthorized());
        var session = login("test-hr");
        mvc.perform(get("/api/v1/auth/session").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("test-hr")).andExpect(jsonPath("$.authorities[0]").value("ROLE_HR"));
        mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/v1/auth/session")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
    @Test void onlyHrCanReadBusinessDataAndOnlyAdminCanProvision() throws Exception {
        for (AccountRole role : AccountRole.values()) {
            var session = login("test-" + role.name().toLowerCase());
            int expected = role == AccountRole.HR ? 200 : 403;
            mvc.perform(get("/api/v1/employees").session(session)).andExpect(status().is(expected));
            mvc.perform(get("/api/v1/employees/directory").session(session)).andExpect(status().is(expected));
            if (role != AccountRole.HR) {
                for (String suffix : new String[]{"", "/documents", "/leave-requests", "/onboarding-tasks", "/history"}) {
                    mvc.perform(get("/api/v1/employees/999" + suffix).session(session)).andExpect(status().isForbidden());
                }
            }
            mvc.perform(get("/unknown-route").session(session)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/accounts").session(session).with(csrf()).contentType("application/json")
                .content("{\"login\":\"new-" + role + "\",\"password\":\"Synthetic provision password!\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().is(role == AccountRole.ADMIN ? 201 : 403));
        }
    }
    @Test void provisioningRejectsInvalidDataAndConflictsWithoutLeakingHashes() throws Exception {
        var session = login("test-admin");
        String valid = "{\"login\":\"synthetic-new\",\"password\":\"Synthetic provision password!\",\"role\":\"HR\"}";
        mvc.perform(post("/api/v1/admin/accounts").session(session).contentType("application/json").content(valid)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/accounts").session(session).with(csrf()).contentType("application/json").content(valid))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/v1/admin/accounts").session(session).with(csrf()).contentType("application/json").content(valid))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Account login is unavailable"));
        mvc.perform(post("/api/v1/admin/accounts").session(session).with(csrf()).contentType("application/json").content(valid.replace("Synthetic provision password!", "short")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/accounts").session(session).with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
    @Test void provisionedAccountsAuthenticateWithExactlyTheirAssignedRole() throws Exception {
        var admin = login("test-admin");
        for (AccountRole role : AccountRole.values()) {
            String name = "provisioned-" + role.name().toLowerCase();
            String body = json.writeValueAsString(java.util.Map.of("login", name, "password", PASSWORD, "role", role.name()));
            mvc.perform(post("/api/v1/admin/accounts").session(admin).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
            var created = login(name);
            mvc.perform(get("/api/v1/auth/session").session(created)).andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities.length()").value(1))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_" + role.name()));
        }
    }
    @Test void accountDirectoryIsAdminOnlyBoundedAndNeverSerializesCredentials() throws Exception {
        mvc.perform(get("/api/v1/admin/accounts")).andExpect(status().isUnauthorized());
        for (AccountRole role : AccountRole.values()) {
            var session = login("test-" + role.name().toLowerCase());
            var response = mvc.perform(get("/api/v1/admin/accounts").session(session))
                .andExpect(status().is(role == AccountRole.ADMIN ? 200 : 403)).andReturn();
            if (role == AccountRole.ADMIN) {
                var body = json.readTree(response.getResponse().getContentAsString());
                assertEquals(4, body.get("items").size());
                assertEquals(4, body.get("totalElements").asInt());
                assertEquals(0, body.get("page").asInt());
                assertEquals(1, body.get("totalPages").asInt());
                for (var item : body.get("items")) {
                    var fields = new java.util.HashSet<String>();
                    item.fieldNames().forEachRemaining(fields::add);
                    assertEquals(java.util.Set.of("id", "login", "role"), fields);
                }
                for (String page : new String[]{"-1", "214748365", "2147483648", "invalid"}) {
                    mvc.perform(get("/api/v1/admin/accounts").param("page", page).session(session)).andExpect(status().isBadRequest());
                }
                mvc.perform(get("/api/v1/admin/accounts?page=1").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(0));
            }
        }
    }
}
