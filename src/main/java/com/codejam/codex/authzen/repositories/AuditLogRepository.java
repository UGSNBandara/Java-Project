package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}
