package com.mdsproject.backend.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private Double amount;
    private String merchant;
    private String category;
    private String status;
    private UUID walletId;
    private String walletName;
    private LocalDateTime createdAt;
}
