package com.mdsproject.backend.services;

import com.mdsproject.backend.models.enums.AssetType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EstimationService {

    /**
     * Estimate EUR value for non-cash assets. This is a simple heuristic stub.
     * In production this could call an AI or external pricing API.
     */
    public double estimateEurValue(AssetType type, String provider, Double amount) {
        if (type == AssetType.CASH) {
            return amount == null ? 0.0 : amount;
        }

        if (amount == null) return 0.0;

        switch (type) {
            case MILES:
                // Typical valuation ranges widely; use conservative 0.01 EUR per mile
                return amount * 0.01;
            case VOUCHER:
                // Assume voucher amount is in provider currency; default 1:1 to EUR
                // If provider contains 'giftcard' or 'store' you might want different rules
                String p = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
                if (p.contains("airline")) {
                    // airline vouchers sometimes have lower resale value -> 0.8
                    return amount * 0.8;
                }
                return amount;
            default:
                return 0.0;
        }
    }
}
