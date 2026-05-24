package com.mdsproject.backend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateWalletRequest {
    @NotBlank
    private String name;

    private String purpose;

    @NotNull
    @Min(0)
    private Double budgetLimit;

    @NotNull
    @Min(0)
    private Double autoApproveThreshold;

    private UUID parentWalletId;
}
