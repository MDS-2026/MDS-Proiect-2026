package com.mdsproject.backend.models;

import com.mdsproject.backend.models.enums.DemocracySessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "democracy_sessions")
@Getter
@Setter
@NoArgsConstructor
public class DemocracySession extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "goal_text", nullable = false, columnDefinition = "TEXT")
    private String goalText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DemocracySessionStatus status = DemocracySessionStatus.COLLECTING;

    @Column(name = "proposals_json", columnDefinition = "TEXT")
    private String proposalsJson;

    @Column(name = "member_count", nullable = false)
    private int memberCount;
}
