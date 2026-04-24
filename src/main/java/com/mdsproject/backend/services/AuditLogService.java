package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.audit.AuditLogResponse;
import com.mdsproject.backend.models.AuditLog;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(AuditAction action, String email, UUID groupId, UUID targetEntityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setPerformedByEmail(email);
        entry.setGroupId(groupId);
        entry.setTargetEntityId(targetEntityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    public List<AuditLogResponse> getGroupAuditLogs(UUID groupId) {
        return auditLogRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction().name(),
                log.getPerformedByEmail(),
                log.getGroupId(),
                log.getTargetEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
