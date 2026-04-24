package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
