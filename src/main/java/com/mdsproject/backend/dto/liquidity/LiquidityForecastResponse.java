package com.mdsproject.backend.dto.liquidity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Cash-flow forecast produced by the Predictive Liquidity Agent for a group.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiquidityForecastResponse {
    private UUID groupId;
    private String groupName;

    /** Sum of liquid (CASH) assets currently pooled by the group. */
    private Double availableCash;
    /** Sum of transactions that are committed but not yet settled (PENDING / PENDING_*). */
    private Double upcomingObligations;
    /** availableCash - upcomingObligations. Negative means a predicted shortfall. */
    private Double projectedBalance;
    /** max(0, upcomingObligations - availableCash). */
    private Double shortfall;

    /** OK | WATCH | CRITICAL. */
    private String severity;
    /** Warning narrative shown to the group. */
    private String message;
    /** Why the recommended swaps were chosen. */
    private String rationale;

    /** Ranked internal ledger swaps that would cover the shortfall. */
    private List<LedgerSwapProposalDto> recommendedSwaps;
    /** Every non-cash asset that could be swapped, for transparency. */
    private List<LedgerSwapProposalDto> swapCandidates;
}
