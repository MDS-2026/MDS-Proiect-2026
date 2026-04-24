package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.audit.AuditLogResponse;
import com.mdsproject.backend.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@PathVariable UUID groupId) {
        return ResponseEntity.ok(auditLogService.getGroupAuditLogs(groupId));
    }
}
