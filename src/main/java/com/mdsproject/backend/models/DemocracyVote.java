package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "democracy_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "voter_email"}))
@Getter
@Setter
@NoArgsConstructor
public class DemocracyVote extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "voter_email", nullable = false)
    private String voterEmail;

    @Column(name = "option_index", nullable = false)
    private int optionIndex;
}
