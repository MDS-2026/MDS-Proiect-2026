package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Assignment of a Root Wallet member to a specific Sub-wallet.
 *
 * <p>Only sub-wallets (wallets with a non-null parent) carry these rows. Root-wallet
 * membership is implied by {@link GroupMembership} — every group member belongs to the
 * root wallet and its main chat. A user listed here gains full active access (chat,
 * approvals, spending) inside the referenced sub-wallet.</p>
 */
@Entity
@Table(
        name = "sub_wallet_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class SubWalletMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
