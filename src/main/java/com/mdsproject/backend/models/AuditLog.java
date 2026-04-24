package com.mdsproject.backend.models;

import com.mdsproject.backend.models.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(nullable = false)
    private String performedByEmail;

    @Column(nullable = false)
    private UUID groupId;

    private UUID targetEntityId;

    @Column(length = 1000)
    private String details;
}
