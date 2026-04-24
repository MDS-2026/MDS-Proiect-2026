package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByWalletId(UUID walletId);
    List<Transaction> findByWalletGroupId(UUID groupId);
}
