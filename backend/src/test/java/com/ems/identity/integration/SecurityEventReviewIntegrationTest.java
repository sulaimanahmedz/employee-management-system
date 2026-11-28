package com.ems.identity.integration;
import com.ems.identity.entity.SecurityEvent;
import com.ems.identity.repository.SecurityEventRepository;
import com.ems.identity.service.AuditTrailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:event-review;DB_CLOSE_DELAY=-1")
class SecurityEventReviewIntegrationTest {
    @Autowired AuditTrailService audit;
    @Autowired SecurityEventRepository events;
    @Test void pagesAreBoundedDescendingAndDisjointWithoutConcurrentWrites() {
        events.deleteAll();
        for(long id=1;id<=21;id++) audit.record(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,1L,id);
        var first=audit.list(0, null, null, null);var second=audit.list(1, null, null, null);
        assertEquals(20,first.size());assertEquals(1,second.size());
        for(int i=0;i<20;i++) assertEquals(21L-i,first.get(i).subjectId());
        assertEquals(1L,second.get(0).subjectId());
        assertTrue(first.stream().noneMatch(e->e.id().equals(second.get(0).id())));
        assertTrue(audit.list(2, null, null, null).isEmpty());
        assertThrows(IllegalArgumentException.class,()->audit.list(-1, null, null, null));
        assertThrows(IllegalArgumentException.class,()->audit.list(Integer.MAX_VALUE, null, null, null));
    }
    @Test void actionFilteringHappensBeforePaginationAndPreservesDescendingOrder() {
        events.deleteAll();
        for(long i=1;i<=21;i++) {
            audit.record(SecurityEvent.Action.PASSWORD_CHANGED,1L,i);
            audit.record(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,1L,i);
        }
        var first=audit.list(0, SecurityEvent.Action.PASSWORD_CHANGED, null, null);
        var second=audit.list(1, SecurityEvent.Action.PASSWORD_CHANGED, null, null);
        assertEquals(20,first.size());assertEquals(1,second.size());
        for(int i=0;i<20;i++) {
            assertEquals(SecurityEvent.Action.PASSWORD_CHANGED,first.get(i).action());
            assertEquals(21L-i,first.get(i).subjectId());
        }
        assertEquals(1L,second.get(0).subjectId());
        assertTrue(audit.list(2, SecurityEvent.Action.PASSWORD_CHANGED, null, null).isEmpty());
        assertTrue(audit.list(0, SecurityEvent.Action.RESET_ISSUED, null, null).isEmpty());
        assertEquals(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,audit.list(0, null, null, null).get(0).action());
    }
    @Test void subjectFilteringPrecedesPagingAndDoesNotMatchActorOrUnattributedEvents() {
        events.deleteAll();
        for(int i=0;i<21;i++) {
            audit.record(SecurityEvent.Action.PASSWORD_CHANGED,7L,9L);
            audit.record(SecurityEvent.Action.PASSWORD_CHANGED,9L,7L);
        }
        audit.record(SecurityEvent.Action.ACCESS_DENIED,null,null);
        audit.record(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,7L,9L);
        var first=audit.list(0,SecurityEvent.Action.PASSWORD_CHANGED,9L, null);
        var second=audit.list(1,SecurityEvent.Action.PASSWORD_CHANGED,9L, null);
        assertEquals(20,first.size());assertEquals(1,second.size());
        assertTrue(first.stream().allMatch(e->e.subjectId()==9L && e.actorId()==7L));
        assertTrue(first.get(19).id()>second.get(0).id());
        assertEquals(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,audit.list(0,null,9L, null).get(0).action());
        assertTrue(audit.list(2,null,9L, null).isEmpty());
        assertTrue(audit.list(0,null,Long.MAX_VALUE, null).isEmpty());
        assertEquals(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,audit.list(0,null,null, null).get(0).action());
    }

    @Test void actorCombinationsFilterBeforePagingAndNeverMatchSubjectOrNullActor() {
        events.deleteAll();
        for(int i=0;i<21;i++) {
            audit.record(SecurityEvent.Action.PASSWORD_CHANGED,7L,9L);
            audit.record(SecurityEvent.Action.PASSWORD_CHANGED,9L,7L);
        }
        audit.record(SecurityEvent.Action.ACCESS_DENIED,null,7L);
        audit.record(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,7L,10L);
        var first=audit.list(0,SecurityEvent.Action.PASSWORD_CHANGED,9L,7L);
        var second=audit.list(1,SecurityEvent.Action.PASSWORD_CHANGED,9L,7L);
        assertEquals(20,first.size());assertEquals(1,second.size());
        assertTrue(first.stream().allMatch(e->e.actorId()==7L && e.subjectId()==9L));
        assertTrue(first.get(19).id()>second.get(0).id());
        assertEquals(SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,audit.list(0,null,null,7L).get(0).action());
        assertEquals(20,audit.list(0,null,9L,7L).size());
        assertEquals(20,audit.list(0,SecurityEvent.Action.PASSWORD_CHANGED,null,7L).size());
        assertTrue(audit.list(0,SecurityEvent.Action.ACCOUNT_ACCESS_CHANGED,9L,7L).isEmpty());
        assertTrue(audit.list(2,null,null,7L).isEmpty());
        assertTrue(audit.list(0,null,null,Long.MAX_VALUE).isEmpty());
    }

}
