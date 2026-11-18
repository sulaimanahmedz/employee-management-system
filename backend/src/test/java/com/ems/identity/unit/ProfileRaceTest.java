package com.ems.identity.unit;
import com.ems.identity.controller.AccountProfileController;
import com.ems.identity.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProfileRaceTest {
 @Test void deletionBetweenFilterValidationAndProfileLookupIsUnauthorized() {
  var accounts=mock(AccountService.class);var controller=new AccountProfileController(accounts);
  var error=assertThrows(ResponseStatusException.class,()->controller.profile(new TestingAuthenticationToken("removed",null)));
  assertEquals(401,error.getStatusCode().value());verify(accounts).findIdentity("removed");
 }
}
