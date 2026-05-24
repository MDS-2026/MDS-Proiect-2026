package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.TransactionGroupApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionGroupApprovalRepository extends JpaRepository<TransactionGroupApproval, UUID> {

    Optional<TransactionGroupApproval> findByTransaction_IdAndUser_Id(UUID transactionId, UUID userId);

    @Query("SELECT COUNT(a) FROM TransactionGroupApproval a WHERE a.transaction.id = :txId")
    long countByTransactionId(@Param("txId") UUID txId);

    @Query("SELECT COUNT(a) FROM TransactionGroupApproval a WHERE a.transaction.id = :txId AND a.approvedAt IS NOT NULL")
    long countApprovedByTransactionId(@Param("txId") UUID txId);
}
