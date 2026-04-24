package com.mdsproject.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private UUID id;
    private String name;
    private String purpose;
    private Double budgetLimit;
    private Double autoApproveThreshold;
    private UUID parentWalletId;
    private UUID groupId;
}
