package com.ems.identity.repository;
import com.ems.identity.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    @org.springframework.data.jpa.repository.Query("select e from SecurityEvent e where e.actorId = :actorId and (:action is null or e.action = :action) and (:subjectId is null or e.subjectId = :subjectId)")
    Page<SecurityEvent> findActorEvents(Long actorId, SecurityEvent.Action action, Long subjectId, Pageable pageable);
    Page<SecurityEvent> findBySubjectId(Long subjectId, Pageable pageable);
    Page<SecurityEvent> findByActionAndSubjectId(SecurityEvent.Action action, Long subjectId, Pageable pageable);
    Page<SecurityEvent> findByAction(SecurityEvent.Action action, Pageable pageable);
}
