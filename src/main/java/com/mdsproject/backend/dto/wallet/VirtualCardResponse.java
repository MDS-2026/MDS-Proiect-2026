package com.mdsproject.backend.dto.wallet;

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
public class VirtualCardResponse {
    private UUID id;
    private String cardNumber;
    private String maskedCardNumber;
    private String cvv;
    private LocalDate expiryDate;
    private Boolean active;
    private UUID walletId;
    private String walletName;
}
