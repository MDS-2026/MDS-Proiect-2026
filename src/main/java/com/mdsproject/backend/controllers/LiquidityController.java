package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.liquidity.ExecuteSwapRequest;
import com.mdsproject.backend.dto.liquidity.LiquidityForecastResponse;
import com.mdsproject.backend.services.PredictiveLiquidityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints for the Predictive Liquidity (Treasury) Agent.
 */
@RestController
@RequestMapping("/api/groups/{groupId}/liquidity")
@RequiredArgsConstructor
public class LiquidityController {

    private final PredictiveLiquidityService liquidityService;

    /**
     * GET /api/groups/{groupId}/liquidity/forecast
     * Returns the current cash-flow forecast, shortfall warning and proposed ledger swaps.
     */
    @GetMapping("/forecast")
    public ResponseEntity<LiquidityForecastResponse> forecast(@PathVariable UUID groupId) {
        return ResponseEntity.ok(liquidityService.forecast(groupId));
    }

    /**
     * POST /api/groups/{groupId}/liquidity/swap
     * Executes an internal ledger swap, converting a non-cash asset into cash, and returns
     * the refreshed forecast.
     */
    @PostMapping("/swap")
    public ResponseEntity<LiquidityForecastResponse> swap(
            @PathVariable UUID groupId,
            @Valid @RequestBody ExecuteSwapRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                liquidityService.executeSwap(groupId, request.getAssetId(), auth.getName()));
    }
}
