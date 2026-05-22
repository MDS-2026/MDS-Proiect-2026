package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transaction_group_approvals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"transaction_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class TransactionGroupApproval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** When set, this member has approved the transaction for group consensus. */
    private LocalDateTime approvedAt;
}
