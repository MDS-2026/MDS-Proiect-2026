package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String purpose;

    @Column(nullable = false)
    private Double budgetLimit;

    @Column(nullable = false)
    private Double autoApproveThreshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_wallet_id")
    private Wallet parentWallet;

    @OneToMany(mappedBy = "parentWallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wallet> subWallets = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FairPayGroup group;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();
}
