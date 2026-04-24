package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VirtualCardRepository extends JpaRepository<VirtualCard, UUID> {
    Optional<VirtualCard> findByWalletId(UUID walletId);
}
