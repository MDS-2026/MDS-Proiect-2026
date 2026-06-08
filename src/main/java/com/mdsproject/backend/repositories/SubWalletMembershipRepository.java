package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.SubWalletMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubWalletMembershipRepository extends JpaRepository<SubWalletMembership, UUID> {

    List<SubWalletMembership> findByWallet_Id(UUID walletId);

    Optional<SubWalletMembership> findByWallet_IdAndUser_Id(UUID walletId, UUID userId);

    boolean existsByWallet_IdAndUser_Id(UUID walletId, UUID userId);

    boolean existsByWallet_IdAndUser_Email(UUID walletId, String userEmail);

    void deleteByWallet_IdAndUser_Id(UUID walletId, UUID userId);
}
