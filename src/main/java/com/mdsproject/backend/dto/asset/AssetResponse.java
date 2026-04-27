package com.mdsproject.backend.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {
    private UUID id;
    private String type;
    private String provider;
    private Double estimatedEurValue;
    private Double amount;
    private String amountUnit;
    private LocalDate expiryDate;
}
