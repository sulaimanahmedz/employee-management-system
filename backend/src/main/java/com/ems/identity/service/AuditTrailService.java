package com.ems.identity.service;
import com.ems.identity.entity.SecurityEvent;
import com.ems.identity.dto.SecurityEventView;
import com.ems.identity.repository.SecurityEventRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {
    private final SecurityEventRepository events;
    public AuditTrailService(SecurityEventRepository events) { this.events = events; }
    @Transactional public void record(SecurityEvent.Action action, Long actor, Long subject) {
        events.saveAndFlush(new SecurityEvent(Instant.now(), action, actor, subject));
    }
    @Transactional(readOnly = true) public List<SecurityEventView> list(int page, SecurityEvent.Action action, Long subjectId, Long actorId) {
        if (page < 0 || page > Integer.MAX_VALUE / 20) throw new IllegalArgumentException("Invalid audit page");
        if (subjectId != null && subjectId <= 0) throw new IllegalArgumentException("Invalid audit subject");
        if (actorId != null && actorId <= 0) throw new IllegalArgumentException("Invalid audit actor");
        var pageable = PageRequest.of(page, 20, Sort.by("id").descending());
        var result = actorId != null ? events.findActorEvents(actorId, action, subjectId, pageable) : subjectId == null
            ? (action == null ? events.findAll(pageable) : events.findByAction(action, pageable))
            : (action == null ? events.findBySubjectId(subjectId, pageable) : events.findByActionAndSubjectId(action, subjectId, pageable));
        return result.stream()
            .map(e -> new SecurityEventView(e.getId(), e.getOccurredAt(), e.getAction(), e.getActorId(), e.getSubjectId())).toList();
    }
}
