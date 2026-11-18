package com.ems.identity.api;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountService;
import com.ems.employee.service.EmployeeService;
import com.ems.employee.dto.EmployeeRequest;
import com.ems.employee.entity.EmployeeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.ApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:scope-api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AccountScopeApiTest {
 @Autowired MockMvc mvc;@Autowired ApplicationContext context;@Autowired AccountService accounts;@Autowired EmployeeService employees;
 @Test void allRolesHaveOnlyTheirAssignedHttpAccessAndCannotEscalate() throws Exception {
  var owner=accounts.provision("scope-owner","Synthetic account password!",AccountRole.EMPLOYEE);
  var employee=employees.create(new EmployeeRequest("Synthetic","Owner","scope-api@example.com","QA","Analyst",EmployeeStatus.ACTIVE));
  String scope="/api/v1/admin/accounts/"+owner.id()+"/scope";
  var admin=IdentityFixture.role(context,"ADMIN");
  mvc.perform(put(scope).with(admin).with(csrf()).contentType("application/json").content("{\"employeeId\":"+employee.id()+",\"teamEmployeeIds\":[]}")).andExpect(status().isOk()).andExpect(jsonPath("$.employeeId").value(employee.id()));
  mvc.perform(get(scope).with(admin)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/accounts/"+owner.id()).with(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
  mvc.perform(post("/api/v1/auth/reset").with(csrf()).contentType("application/json").content("{\"token\":\"invalid\",\"newPassword\":\"short\"}")).andExpect(status().isBadRequest());
  mvc.perform(put(scope).with(admin).with(csrf()).contentType("application/json").content("{\"employeeId\":-1,\"teamEmployeeIds\":[null]}")).andExpect(status().isBadRequest());
  mvc.perform(put(scope).with(admin).contentType("application/json").content("{\"employeeId\":null,\"teamEmployeeIds\":[]}")).andExpect(status().isForbidden());
  for(var role:AccountRole.values()) {
   var auth=IdentityFixture.role(context,role.name());
   if(role!=AccountRole.ADMIN) {
    mvc.perform(get(scope).with(auth)).andExpect(status().isForbidden());
    mvc.perform(put(scope).with(auth).with(csrf()).contentType("application/json").content("{\"employeeId\":null,\"teamEmployeeIds\":[]}")).andExpect(status().isForbidden());
   }
   mvc.perform(get("/api/v1/self/employees").with(auth)).andExpect(status().is(role==AccountRole.MANAGER||role==AccountRole.EMPLOYEE?200:403));
   mvc.perform(get("/api/v1/self/employees/"+employee.id()).with(auth)).andExpect(status().isForbidden());
   mvc.perform(put("/api/v1/self/employees/"+employee.id()).with(auth).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  }
  var result=mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username","scope-owner").param("password","Synthetic account password!")).andExpect(status().isNoContent()).andReturn();
  var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/v1/self/employees").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(employee.id()));
  mvc.perform(get("/api/v1/self/employees/"+employee.id()).session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("scope-api@example.com"));
  mvc.perform(get("/api/v1/self/employees/999999").session(session)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/self/employees/"+employee.id()+"/documents").session(session)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/self/employees")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/admin/audit?page=-1").with(admin)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/admin/audit?page=2147483647").with(admin)).andExpect(status().isBadRequest());
 }
}
