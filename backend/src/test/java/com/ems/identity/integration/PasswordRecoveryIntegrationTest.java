package com.ems.identity.integration;
import com.ems.identity.entity.AccountRole;
import com.ems.identity.service.AccountService;
import com.ems.identity.service.AccountLifecycleService;
import com.ems.identity.repository.AccountRepository;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:recovery-integration;DB_CLOSE_DELAY=-1")
class PasswordRecoveryIntegrationTest {
 @Autowired AccountService accounts;@Autowired AccountLifecycleService lifecycle;@Autowired AccountRepository repository;
 @Test void replacementInvalidatesPreviousTokenAndOnlyOneConcurrentUseCanCommit() throws Exception {
  var account=accounts.provision("recovery-race","Synthetic recovery password!",AccountRole.EMPLOYEE);
  var first=lifecycle.issueReset(99L,account.id());var second=lifecycle.issueReset(99L,account.id());
  assertNotEquals(first.token(),second.token());assertThrows(ResponseStatusException.class,()->lifecycle.reset(first.token(),"Synthetic replacement password!"));
  var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  java.util.concurrent.Callable<Boolean> attempt=()->{start.await();try{lifecycle.reset(second.token(),"Synthetic replacement password!");return true;}catch(ResponseStatusException rejected){assertEquals(400,rejected.getStatusCode().value());return false;}};
  try {
   var a=executor.submit(attempt);var b=executor.submit(attempt);start.countDown();
   assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS),"Exactly one reset transaction commits");
  }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));}
  var stored=repository.findById(account.id()).orElseThrow();assertEquals(1,stored.getSecurityVersion());assertNull(stored.getResetDigest());
  assertTrue(accounts.verifyCredentials(account.login(),"Synthetic replacement password!").isPresent());
  assertTrue(accounts.verifyCredentials(account.login(),"Synthetic recovery password!").isEmpty());
 }
 @Test void rejectedReplacementRollsBackWithoutConsumingTokenOrChangingPassword() {
  var account=accounts.provision("recovery-rollback","Synthetic recovery password!",AccountRole.EMPLOYEE);
  var ticket=lifecycle.issueReset(99L,account.id());
  assertThrows(IllegalArgumentException.class,()->lifecycle.reset(ticket.token(),"short"));
  var stored=repository.findById(account.id()).orElseThrow();assertNotNull(stored.getResetDigest());assertEquals(0,stored.getSecurityVersion());
  lifecycle.reset(ticket.token(),"Synthetic final password!");assertTrue(accounts.verifyCredentials(account.login(),"Synthetic final password!").isPresent());
 }
}
