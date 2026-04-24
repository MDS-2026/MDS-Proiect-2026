package com.mdsproject.backend.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private String action;
    private String performedByEmail;
    private UUID groupId;
    private UUID targetEntityId;
    private String details;
    private LocalDateTime createdAt;
}
