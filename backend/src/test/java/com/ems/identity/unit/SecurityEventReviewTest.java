package com.ems.identity.unit;
import com.ems.identity.repository.SecurityEventRepository;
import com.ems.identity.service.AuditTrailService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Pageable;
class SecurityEventReviewTest {
    @Test void invalidOffsetsNeverReachPersistenceAndStorageFailuresAreNotPresentedAsEmptyResults() {
        var repository=mock(SecurityEventRepository.class);
        var service=new AuditTrailService(repository);
        assertThrows(IllegalArgumentException.class,()->service.list(-1));
        assertThrows(IllegalArgumentException.class,()->service.list(Integer.MAX_VALUE));
        verifyNoInteractions(repository);
        var failure=new IllegalStateException("synthetic unavailable store");
        when(repository.findAll(any(Pageable.class))).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(0)));
    }
}
