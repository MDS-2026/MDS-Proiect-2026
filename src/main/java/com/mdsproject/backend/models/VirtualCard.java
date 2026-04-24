package com.mdsproject.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "virtual_cards")
@Getter
@Setter
@NoArgsConstructor
public class VirtualCard extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private String cvv;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, unique = true)
    private Wallet wallet;
}
