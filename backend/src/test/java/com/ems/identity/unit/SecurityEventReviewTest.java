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
        assertThrows(IllegalArgumentException.class,()->service.list(-1, null, null));
        assertThrows(IllegalArgumentException.class,()->service.list(Integer.MAX_VALUE, null, null));
        verifyNoInteractions(repository);
        var failure=new IllegalStateException("synthetic unavailable store");
        when(repository.findAll(any(Pageable.class))).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(0, null, null)));
    }
    @Test void filteredReadsKeepThePageBoundAndPropagateStorageFailure() {
        var repository=mock(SecurityEventRepository.class);var service=new AuditTrailService(repository);
        var action=com.ems.identity.entity.SecurityEvent.Action.PASSWORD_RESET;
        assertThrows(IllegalArgumentException.class,()->service.list(-1, action, null));verifyNoInteractions(repository);
        var failure=new IllegalStateException("synthetic unavailable filter");
        var pageable=org.springframework.data.domain.PageRequest.of(2,20,org.springframework.data.domain.Sort.by("id").descending());
        when(repository.findByAction(action,pageable)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(2, action, null)));
        verify(repository).findByAction(action,pageable);verifyNoMoreInteractions(repository);
    }
    @Test void subjectValidationAndCombinedQueriesPreserveBoundsAndFailures() {
        var repository=mock(SecurityEventRepository.class);var service=new AuditTrailService(repository);
        for(long id:new long[]{0,-1}) assertThrows(IllegalArgumentException.class,()->service.list(0,null,id));
        verifyNoInteractions(repository);
        var pageable=org.springframework.data.domain.PageRequest.of(1,20,org.springframework.data.domain.Sort.by("id").descending());
        var failure=new IllegalStateException("unavailable");
        when(repository.findBySubjectId(9L,pageable)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(1,null,9L)));
        var action=com.ems.identity.entity.SecurityEvent.Action.PASSWORD_RESET;
        when(repository.findByActionAndSubjectId(action,9L,pageable)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.list(1,action,9L)));
        verify(repository).findBySubjectId(9L,pageable);verify(repository).findByActionAndSubjectId(action,9L,pageable);
        verifyNoMoreInteractions(repository);
    }

}
