package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.transaction.CreateTransactionRequest;
import com.mdsproject.backend.dto.transaction.TransactionResponse;
import com.mdsproject.backend.dto.transaction.ValidateTransactionRequest;
import com.mdsproject.backend.dto.transaction.ValidateTransactionResponse;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.models.enums.TransactionStatus;
import com.mdsproject.backend.repositories.TransactionRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AuditLogService auditLogService;
    private final AiValidationService aiValidationService;
    private final AlertService alertService;

    @Transactional
    public TransactionResponse createTransaction(UUID walletId, CreateTransactionRequest request, String email) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setMerchant(request.getMerchant());
        tx.setCategory(request.getCategory());
        tx.setWallet(wallet);

        try {
            boolean valid = aiValidationService.validateTransaction(
                    wallet,
                    request.getMerchant(),
                    request.getCategory(),
                    request.getAmount(),
                    email
            );

            if (!valid) {
                tx.setStatus(TransactionStatus.DECLINED);
                transactionRepository.save(tx);

                String reason = aiValidationService.getValidationReason(wallet, request.getMerchant(), request.getCategory());
                alertService.alertTransactionDeclined(tx, "AI Warning: " + reason);

                auditLogService.log(AuditAction.TRANSACTION_DECLINED, email,
                        wallet.getGroup().getId(), tx.getId(),
                        "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                                + " on wallet '" + wallet.getName() + "' declined by AI — Reason: " + reason);

                return toResponse(tx);
            }

            // Auto-approve if amount is within threshold
            if (request.getAmount() <= wallet.getAutoApproveThreshold()) {
                tx.setStatus(TransactionStatus.APPROVED);
            } else {
                tx.setStatus(TransactionStatus.PENDING);
            }

        } catch (Exception e) {
            tx.setStatus(TransactionStatus.PENDING_MANUAL_APPROVAL);
            transactionRepository.save(tx);

            alertService.alertTransactionDeclined(tx, "AI validation unavailable — Awaiting manual approval");

            auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                    wallet.getGroup().getId(), tx.getId(),
                    "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                            + " on wallet '" + wallet.getName() + "' set to PENDING_MANUAL_APPROVAL (AI system error)");

            return toResponse(tx);
        }

        transactionRepository.save(tx);

        auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                wallet.getGroup().getId(), tx.getId(),
                "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                        + " on wallet '" + wallet.getName() + "' — Status: " + tx.getStatus());

        if (tx.getStatus() == TransactionStatus.PENDING) {
            alertService.alertTransactionDeclined(tx, "New transaction pending approval: €" + tx.getAmount() + " at " + tx.getMerchant());
        }

        return toResponse(tx);
    }

    public List<TransactionResponse> getWalletTransactions(UUID walletId) {
        return transactionRepository.findByWalletId(walletId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getGroupTransactions(UUID groupId) {
        return transactionRepository.findByWalletGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ValidateTransactionResponse validateTransaction(ValidateTransactionRequest request, String email) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        boolean valid = aiValidationService.validateTransaction(
                wallet,
                request.getMerchant(),
                request.getCategory(),
                request.getAmount(),
                email
        );

        String reason = valid
                ? "Transaction matches wallet purpose"
                : aiValidationService.getValidationReason(wallet, request.getMerchant(), request.getCategory());

        if (!valid) {
            // ... (existing code for decline)
            Transaction dummyTx = new Transaction();
            dummyTx.setMerchant(request.getMerchant());
            dummyTx.setAmount(request.getAmount());
            dummyTx.setWallet(wallet);
            alertService.alertTransactionDeclined(dummyTx, "AI Warning: " + reason);
        } else {
            alertService.sendGroupAlert(wallet.getGroup(), "AI Approved: Transaction of €" + request.getAmount() + " at " + request.getMerchant() + " matches wallet purpose.", "/groups/" + wallet.getGroup().getId());
        }

        return new ValidateTransactionResponse(valid, reason);
    }

    @Transactional
    public TransactionResponse approveTransaction(UUID txId, String email) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        tx.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(tx);

        auditLogService.log(AuditAction.TRANSACTION_APPROVED, email,
                tx.getWallet().getGroup().getId(), tx.getId(),
                "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant() + " approved");

        alertService.sendGroupAlert(tx.getWallet().getGroup(), "Transaction Approved: €" + tx.getAmount() + " at " + tx.getMerchant(), "/groups/" + tx.getWallet().getGroup().getId());

        return toResponse(tx);
    }

    @Transactional
    public TransactionResponse declineTransaction(UUID txId, String email) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        tx.setStatus(TransactionStatus.DECLINED);
        transactionRepository.save(tx);

        auditLogService.log(AuditAction.TRANSACTION_DECLINED, email,
                tx.getWallet().getGroup().getId(), tx.getId(),
                "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant() + " declined");

        // Trigger notification for all group members
        alertService.alertTransactionDeclined(tx, "Manually declined by administrator (" + email + ")");

        return toResponse(tx);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getAmount(),
                tx.getMerchant(),
                tx.getCategory(),
                tx.getStatus().name(),
                tx.getWallet().getId(),
                tx.getWallet().getName(),
                tx.getCreatedAt()
        );
    }
}
