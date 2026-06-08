package com.mdsproject.backend.dto.liquidity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A single proposed internal ledger swap: convert a non-cash asset (e.g. a gift card)
 * into liquid cash so the group can settle upcoming transactions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LedgerSwapProposalDto {
    private UUID assetId;
    private String assetType;
    private String provider;
    /** EUR cash the group would gain by swapping this asset internally. */
    private Double cashValueEur;
    private LocalDate expiryDate;
    private Integer daysToExpiry;
    /** Human-readable reason this swap was recommended (e.g. "expires in 12 days"). */
    private String reason;
}
