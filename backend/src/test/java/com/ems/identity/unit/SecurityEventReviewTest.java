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
        assertThrows(IllegalArgumentException.class,()->service.list(-1, null));
        assertThrows(IllegalArgumentException.class,()->service.list(Integer.MAX_VALUE, null));
        verifyNoInteractions(repository);
        var failure=new IllegalStateException("synthetic unavailable store");
        when(repository.findAll(any(Pageable.class))).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(0, null)));
    }
    @Test void filteredReadsKeepThePageBoundAndPropagateStorageFailure() {
        var repository=mock(SecurityEventRepository.class);var service=new AuditTrailService(repository);
        var action=com.ems.identity.entity.SecurityEvent.Action.PASSWORD_RESET;
        assertThrows(IllegalArgumentException.class,()->service.list(-1,action));verifyNoInteractions(repository);
        var failure=new IllegalStateException("synthetic unavailable filter");
        var pageable=org.springframework.data.domain.PageRequest.of(2,20,org.springframework.data.domain.Sort.by("id").descending());
        when(repository.findByAction(action,pageable)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(2,action)));
        verify(repository).findByAction(action,pageable);verifyNoMoreInteractions(repository);
    }
}
