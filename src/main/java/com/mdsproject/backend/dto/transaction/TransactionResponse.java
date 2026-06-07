package com.mdsproject.backend.dto.transaction;

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
public class TransactionResponse {
    private UUID id;
    private Double amount;
    private String merchant;
    private String category;
    private String status;
    private UUID walletId;
    private String walletName;
    private LocalDateTime createdAt;

    /**
     * Populated when {@code status} is {@code PENDING_GROUP_APPROVAL} or {@code PENDING_MANUAL_APPROVAL}:
     * approvals required for consensus (⌊memberCount/2⌋+1).
     */
    private Integer groupConsensusRequired;

    /**
     * Populated when {@code status} is {@code PENDING_GROUP_APPROVAL} or {@code PENDING_MANUAL_APPROVAL}:
     * how many members have approved so far.
     */
    private Integer groupConsensusApproved;
}
