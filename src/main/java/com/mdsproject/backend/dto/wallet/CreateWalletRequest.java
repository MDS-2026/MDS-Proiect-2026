package com.mdsproject.backend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Double budgetLimit;

    @NotNull
    private Double autoApproveThreshold;

    private UUID parentWalletId;
}
