package com.mdsproject.backend.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ValidateTransactionRequest {
    @NotNull
    private UUID walletId;

    @NotNull
    private Double amount;

    @NotBlank
    private String merchant;

    private String category;
}
