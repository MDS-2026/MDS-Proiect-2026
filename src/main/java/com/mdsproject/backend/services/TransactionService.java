package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.transaction.CreateTransactionRequest;
import com.mdsproject.backend.dto.transaction.TransactionResponse;
import com.mdsproject.backend.dto.transaction.ValidateTransactionRequest;
import com.mdsproject.backend.dto.transaction.ValidateTransactionResponse;
import com.mdsproject.backend.exceptions.BadRequestException;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.GroupMembership;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.TransactionGroupApproval;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.models.enums.TransactionStatus;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import com.mdsproject.backend.repositories.TransactionGroupApprovalRepository;
import com.mdsproject.backend.repositories.TransactionRepository;
import com.mdsproject.backend.repositories.UserRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final GroupMembershipRepository groupMembershipRepository;
    private final TransactionGroupApprovalRepository transactionGroupApprovalRepository;
    private final UserRepository userRepository;
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
                    email);

            if (!valid) {
                // AI mismatch: trimite la aprobare de grup, nu DECLINED
                tx.setStatus(TransactionStatus.PENDING_GROUP_APPROVAL);
                transactionRepository.save(tx);

                // Seed consensus approvals so that group members can vote
                seedGroupConsensusApprovals(tx, wallet.getGroup().getId());

                String reason = aiValidationService.getValidationReason(wallet, request.getMerchant(),
                        request.getCategory());
                alertService.alertTransactionDeclined(tx, "AI Warning: " + reason + " — Group approval required");

                auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                        wallet.getGroup().getId(), tx.getId(),
                        "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                                + " on wallet '" + wallet.getName()
                                + "' — Status: PENDING_GROUP_APPROVAL (AI mismatch: " + reason + ")");

                int n = (int) transactionGroupApprovalRepository.countByTransactionId(tx.getId());
                String memberSummary = buildMemberEmailSummary(wallet.getGroup().getId());
                auditLogService.log(AuditAction.TRANSACTION_GROUP_CONSENSUS_REQUESTED, email,
                        wallet.getGroup().getId(), tx.getId(),
                        "Approval requests sent to all " + n + " group member(s) for this transaction. "
                                + memberSummary);

                return toResponse(tx);
            }

            // Auto-approve dacă suma e sub prag
            if (request.getAmount() <= wallet.getAutoApproveThreshold()) {
                tx.setStatus(TransactionStatus.APPROVED);
                deductFromWalletHierarchy(wallet, request.getAmount());
            } else {
                tx.setStatus(TransactionStatus.PENDING);
            }

        } catch (Exception e) {
            tx.setStatus(TransactionStatus.PENDING_GROUP_APPROVAL);
            transactionRepository.save(tx);

            seedGroupConsensusApprovals(tx, wallet.getGroup().getId());

            alertService.alertTransactionDeclined(tx, "AI validation unavailable — unanimous group approval required");

            auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                    wallet.getGroup().getId(), tx.getId(),
                    "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                            + " on wallet '" + wallet.getName() + "' — Status: PENDING_GROUP_APPROVAL"
                            + " (AI service unavailable; unanimous group approval required)");

            int n = (int) transactionGroupApprovalRepository.countByTransactionId(tx.getId());
            String memberSummary = buildMemberEmailSummary(wallet.getGroup().getId());
            auditLogService.log(AuditAction.TRANSACTION_GROUP_CONSENSUS_REQUESTED, email,
                    wallet.getGroup().getId(), tx.getId(),
                    "Approval requests sent to all " + n + " group member(s) for this transaction. " + memberSummary);

            return toResponse(tx);
        }

        if (request.getAmount() <= wallet.getAutoApproveThreshold()) {
            tx.setStatus(TransactionStatus.APPROVED);
            deductFromWalletHierarchy(wallet, request.getAmount());
        } else {
            tx.setStatus(TransactionStatus.PENDING);
        }

        transactionRepository.save(tx);

        auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                wallet.getGroup().getId(), tx.getId(),
                "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                        + " on wallet '" + wallet.getName() + "' — Status: " + tx.getStatus());

        if (tx.getStatus() == TransactionStatus.PENDING) {
            alertService.alertTransactionDeclined(tx,
                    "New transaction pending approval: €" + tx.getAmount() + " at " + tx.getMerchant());
        }

        return toResponse(tx);
    }

    private String buildMemberEmailSummary(UUID groupId) {
        List<GroupMembership> members = groupMembershipRepository.findByGroupId(groupId);
        String emails = members.stream()
                .map(m -> m.getUser().getEmail())
                .collect(Collectors.joining(", "));
        return "Members: " + emails + ".";
    }

    private void seedGroupConsensusApprovals(Transaction tx, UUID groupId) {
        List<GroupMembership> members = groupMembershipRepository.findByGroupId(groupId);
        for (GroupMembership membership : members) {
            TransactionGroupApproval row = new TransactionGroupApproval();
            row.setTransaction(tx);
            row.setUser(membership.getUser());
            transactionGroupApprovalRepository.save(row);
        }
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

        boolean valid;
        String reason;

        try {
            valid = aiValidationService.validateTransaction(
                    wallet,
                    request.getMerchant(),
                    request.getCategory(),
                    request.getAmount(),
                    email);
            reason = valid
                    ? "Transaction matches wallet purpose"
                    : aiValidationService.getValidationReason(wallet, request.getMerchant(), request.getCategory());
        } catch (AiValidationService.TransactionValidationException e) {
            return new ValidateTransactionResponse(false, "AI service unavailable — group approval will be required");
        }

        if (!valid) {
            Transaction dummyTx = new Transaction();
            dummyTx.setMerchant(request.getMerchant());
            dummyTx.setAmount(request.getAmount());
            dummyTx.setWallet(wallet);
            alertService.alertTransactionDeclined(dummyTx, "AI Warning: " + reason);
        } else {
            alertService.sendGroupAlert(
                    wallet.getGroup(), "AI Approved: Transaction of €" + request.getAmount() + " at "
                            + request.getMerchant() + " matches wallet purpose.",
                    "/groups/" + wallet.getGroup().getId());
        }

        return new ValidateTransactionResponse(valid, reason);
    }

    @Transactional
    public TransactionResponse approveTransaction(UUID txId, String email) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getStatus() == TransactionStatus.DECLINED) {
            throw new BadRequestException("Transaction is already declined");
        }
        if (tx.getStatus() == TransactionStatus.APPROVED) {
            return toResponse(tx);
        }

        if (tx.getStatus() == TransactionStatus.PENDING_GROUP_APPROVAL) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            TransactionGroupApproval row = transactionGroupApprovalRepository
                    .findByTransaction_IdAndUser_Id(txId, user.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "You are not in the approval set for this transaction (only current group members when it was created can approve)."));

            if (row.getApprovedAt() == null) {
                row.setApprovedAt(LocalDateTime.now());
                transactionGroupApprovalRepository.save(row);
            }

            long required = transactionGroupApprovalRepository.countByTransactionId(txId);
            long approved = transactionGroupApprovalRepository.countApprovedByTransactionId(txId);

            if (required > 0 && approved >= required) {
                tx.setStatus(TransactionStatus.APPROVED);
                deductFromWalletHierarchy(tx.getWallet(), tx.getAmount());
                transactionRepository.save(tx);
                auditLogService.log(AuditAction.TRANSACTION_APPROVED, email,
                        tx.getWallet().getGroup().getId(), tx.getId(),
                        "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                                + " approved after unanimous group consensus (" + approved + "/" + required + ")");
            }

            return toResponse(tx);
        }

        // Legacy single-step approval for PENDING (over threshold, AI OK)
        tx.setStatus(TransactionStatus.APPROVED);
        deductFromWalletHierarchy(tx.getWallet(), tx.getAmount());
        transactionRepository.save(tx);

        auditLogService.log(AuditAction.TRANSACTION_APPROVED, email,
                tx.getWallet().getGroup().getId(), tx.getId(),
                "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant() + " approved");

        alertService.sendGroupAlert(tx.getWallet().getGroup(),
                "Transaction Approved: €" + tx.getAmount() + " at " + tx.getMerchant(),
                "/groups/" + tx.getWallet().getGroup().getId());

        return toResponse(tx);
    }

    @Transactional
    public TransactionResponse declineTransaction(UUID txId, String email) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (tx.getStatus() == TransactionStatus.DECLINED) {
            return toResponse(tx);
        }
        if (tx.getStatus() == TransactionStatus.APPROVED) {
            throw new BadRequestException("Cannot decline an approved transaction");
        }

        UUID groupId = tx.getWallet().getGroup().getId();

        if (tx.getStatus() == TransactionStatus.PENDING_GROUP_APPROVAL) {
            if (!groupMembershipRepository.existsByUserEmailAndGroupId(email, groupId)) {
                throw new BadRequestException("Only group members can decline this transaction");
            }
        }

        tx.setStatus(TransactionStatus.DECLINED);
        transactionRepository.save(tx);

        auditLogService.log(AuditAction.TRANSACTION_DECLINED, email,
                groupId, tx.getId(),
                "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant() + " declined");

        // Trigger notification for all group members
        alertService.alertTransactionDeclined(tx, "Manually declined by administrator (" + email + ")");

        return toResponse(tx);
    }

    private TransactionResponse toResponse(Transaction tx) {
        Integer groupConsensusRequired = null;
        Integer groupConsensusApproved = null;
        if (tx.getStatus() == TransactionStatus.PENDING_GROUP_APPROVAL) {
            groupConsensusRequired = (int) transactionGroupApprovalRepository.countByTransactionId(tx.getId());
            groupConsensusApproved = (int) transactionGroupApprovalRepository.countApprovedByTransactionId(tx.getId());
        }

        return new TransactionResponse(
                tx.getId(),
                tx.getAmount(),
                tx.getMerchant(),
                tx.getCategory(),
                tx.getStatus().name(),
                tx.getWallet().getId(),
                tx.getWallet().getName(),
                tx.getCreatedAt(),
                groupConsensusRequired,
                groupConsensusApproved);
    }

    /**
     * Scade suma tranzacției din wallet-ul sursă și din root wallet.
     * Dacă wallet-ul sursă este chiar root-ul, se scade o singură dată.
     */
    private void deductFromWalletHierarchy(Wallet sourceWallet, Double amount) {
        // Scade din wallet-ul sursă
        sourceWallet.setBudgetLimit(sourceWallet.getBudgetLimit() - amount);
        walletRepository.save(sourceWallet);

        // Găsește root wallet-ul (cel fără parent)
        Wallet root = sourceWallet;
        while (root.getParentWallet() != null) {
            root = root.getParentWallet();
        }

        // Scade și din root wallet dacă e diferit de wallet-ul sursă
        if (!root.getId().equals(sourceWallet.getId())) {
            root.setBudgetLimit(root.getBudgetLimit() - amount);
            walletRepository.save(root);
        }
    }
}
