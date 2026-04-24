package com.mdsproject.backend.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private Double estimatedEurValue;

    private LocalDate expiryDate;
}
