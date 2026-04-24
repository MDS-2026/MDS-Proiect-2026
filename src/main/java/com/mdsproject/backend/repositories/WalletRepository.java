package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByGroupId(UUID groupId);
}
