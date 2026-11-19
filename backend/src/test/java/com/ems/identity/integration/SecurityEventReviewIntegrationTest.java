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
        var first=audit.list(0);var second=audit.list(1);
        assertEquals(20,first.size());assertEquals(1,second.size());
        for(int i=0;i<20;i++) assertEquals(21L-i,first.get(i).subjectId());
        assertEquals(1L,second.get(0).subjectId());
        assertTrue(first.stream().noneMatch(e->e.id().equals(second.get(0).id())));
        assertTrue(audit.list(2).isEmpty());
        assertThrows(IllegalArgumentException.class,()->audit.list(-1));
        assertThrows(IllegalArgumentException.class,()->audit.list(Integer.MAX_VALUE));
    }
}
