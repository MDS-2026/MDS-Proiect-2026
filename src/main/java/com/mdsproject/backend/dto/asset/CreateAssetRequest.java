package com.mdsproject.backend.dto.asset;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateAssetRequest {
    @NotBlank
    private String type; // CASH, MILES, VOUCHER

    @NotBlank
    private String provider;

    // For non-cash assets the client may omit estimatedEurValue and provide amount/amountUnit
    private Double estimatedEurValue;

    private Double amount;

    private String amountUnit;

    private LocalDate expiryDate;
}
