package com.mdsproject.backend.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTransactionRequest {
    @NotNull
    private Double amount;

    @NotBlank
    private String merchant;

    private String category;
}
