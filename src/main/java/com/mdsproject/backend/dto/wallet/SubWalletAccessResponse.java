package com.mdsproject.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The calling user's access level for a given wallet, so the frontend can decide which
 * controls (chat box, approve buttons, card details) to render versus a read-only ledger.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubWalletAccessResponse {
    private UUID walletId;
    private boolean subWallet;
    private boolean admin;
    /** Full active access: chat, approvals, spending. */
    private boolean canParticipate;
    /** Read-only visibility of the ledger. */
    private boolean canViewLedger;
}
