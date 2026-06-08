package com.mdsproject.backend.dto.liquidity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to execute a single internal ledger swap: convert the given non-cash
 * asset into liquid cash on the group ledger.
 */
@Data
@NoArgsConstructor
public class ExecuteSwapRequest {
    @NotNull
    private UUID assetId;
}
