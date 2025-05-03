package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByUserId(String userId);
    List<AuditEvent> findByUsername(String username);
    List<AuditEvent> findByEventType(String eventType);
    List<AuditEvent> findByCreatedAtAfter(Timestamp timestamp);
}
