package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "member_constraints")
@Getter
@Setter
@NoArgsConstructor
public class MemberConstraint extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "member_email", nullable = false)
    private String memberEmail;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "deal_breakers", columnDefinition = "TEXT")
    private String dealBreakers;

    @Column(name = "max_contribution")
    private Double maxContribution;
}
