package com.ems.identity.api;

import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountService;
import com.ems.identity.repository.SecurityEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:lifecycle-api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AccountLifecycleApiTest {
    @Autowired MockMvc mvc;
    @Autowired AccountService accounts;
    @Autowired ObjectMapper json;
    @Autowired SecurityEventRepository events;
    static final String PASSWORD="Synthetic lifecycle password!";
    MockHttpSession login(String name,String password) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username",name).param("password",password))
            .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }
    @Test void passwordChangeRevokesEverySessionAndRequiresCurrentPasswordAndCsrf() throws Exception {
        accounts.provision("change-target",PASSWORD,AccountRole.EMPLOYEE);
        var first=login("change-target",PASSWORD);var second=login("change-target",PASSWORD);
        String body=json.writeValueAsString(java.util.Map.of("currentPassword",PASSWORD,"newPassword",PASSWORD+"new"));
        mvc.perform(post("/api/v1/auth/password").session(first).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/password").session(first).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Synthetic replacement password!\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/password").session(first).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNoContent());
        for(var session:new MockHttpSession[]{first,second}) {mvc.perform(get("/api/v1/auth/profile").session(session)).andExpect(status().isUnauthorized());assertTrue(session.isInvalid());}
        mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username","change-target").param("password",PASSWORD)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/profile").session(login("change-target",PASSWORD+"new"))).andExpect(status().isOk());
    }
    @Test void roleAndAccountChangesRevokeSessionsAndRecoveryIsSingleUse() throws Exception {
        accounts.provision("lifecycle-admin",PASSWORD,AccountRole.ADMIN);
        var target=accounts.provision("lifecycle-target",PASSWORD,AccountRole.HR);
        var admin=login("lifecycle-admin",PASSWORD);var old=login("lifecycle-target",PASSWORD);
        String path="/api/v1/admin/accounts/"+target.id();
        mvc.perform(put(path).session(admin).with(csrf()).contentType("application/json").content("{\"role\":\"EMPLOYEE\",\"enabled\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/employees").session(old)).andExpect(status().isUnauthorized());
        var current=login("lifecycle-target",PASSWORD);
        mvc.perform(get("/api/v1/employees").session(current)).andExpect(status().isForbidden());
        var response=mvc.perform(post(path+"/reset").session(admin).with(csrf())).andExpect(status().isOk()).andReturn();
        String token=json.readTree(response.getResponse().getContentAsString()).get("token").asText();
        String reset=json.writeValueAsString(java.util.Map.of("token",token,"newPassword",PASSWORD+"reset"));
        mvc.perform(post("/api/v1/auth/reset").contentType("application/json").content(reset)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/reset").with(csrf()).contentType("application/json").content(reset)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/reset").with(csrf()).contentType("application/json").content(reset)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/auth/profile").session(current)).andExpect(status().isUnauthorized());
        var resetSession=login("lifecycle-target",PASSWORD+"reset");
        mvc.perform(put(path).session(admin).with(csrf()).contentType("application/json").content("{\"role\":\"EMPLOYEE\",\"enabled\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/profile").session(resetSession)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username","lifecycle-target").param("password",PASSWORD+"reset")).andExpect(status().isUnauthorized());
        var audit=mvc.perform(get("/api/v1/admin/audit").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(audit.contains("ACCOUNT_ACCESS_CHANGED"));assertFalse(audit.contains(token));assertFalse(audit.contains(PASSWORD));assertTrue(events.count()>0);
    }
    @Test void anonymousAndNonAdministratorsCannotManageAccountsOrAudit() throws Exception {
        for(var role:AccountRole.values()) {
            accounts.provision("permissions-"+role,PASSWORD,role);var session=login("permissions-"+role,PASSWORD);
            int expected=role==AccountRole.ADMIN?404:403;
            mvc.perform(get("/api/v1/admin/accounts/999999").session(session)).andExpect(status().is(expected));
            mvc.perform(get("/api/v1/admin/audit").session(session)).andExpect(status().is(role==AccountRole.ADMIN?200:403));
            if(role!=AccountRole.ADMIN) {
                mvc.perform(put("/api/v1/admin/accounts/999999").session(session).with(csrf()).contentType("application/json").content("{\"role\":\"ADMIN\",\"enabled\":true}")).andExpect(status().isForbidden());
                mvc.perform(post("/api/v1/admin/accounts/999999/reset").session(session).with(csrf())).andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/api/v1/admin/audit")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/password").with(csrf()).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
    }
    @Test void auditActionFilterRejectsUnknownValuesAndKeepsEveryRoleBoundary() throws Exception {
        accounts.provision("filter-admin",PASSWORD,AccountRole.ADMIN);var admin=login("filter-admin",PASSWORD);
        mvc.perform(get("/api/v1/admin/audit").session(admin).param("action","CREDENTIALS_ACCEPTED"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].action").value("CREDENTIALS_ACCEPTED"));
        mvc.perform(get("/api/v1/admin/audit").session(admin).param("action","UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/audit").session(admin).param("action","PASSWORD_RESET").param("page","-1")).andExpect(status().isBadRequest());
        for(var role:new AccountRole[]{AccountRole.HR,AccountRole.MANAGER,AccountRole.EMPLOYEE}) {
            accounts.provision("filter-"+role,PASSWORD,role);
            mvc.perform(get("/api/v1/admin/audit").session(login("filter-"+role,PASSWORD)).param("action","PASSWORD_RESET")).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/admin/audit").param("action","PASSWORD_RESET")).andExpect(status().isUnauthorized());
    }
}
