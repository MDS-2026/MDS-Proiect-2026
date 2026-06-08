package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.transaction.CreateTransactionRequest;
import com.mdsproject.backend.dto.transaction.TransactionResponse;
import com.mdsproject.backend.dto.transaction.ValidateTransactionRequest;
import com.mdsproject.backend.dto.transaction.ValidateTransactionResponse;
import com.mdsproject.backend.exceptions.BadRequestException;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.TransactionGroupApproval;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.models.enums.TransactionStatus;
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
    private final TransactionGroupApprovalRepository transactionGroupApprovalRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final PredictiveLiquidityService predictiveLiquidityService;
    private final SubWalletAccessService subWalletAccessService;

    @Transactional
    public TransactionResponse createTransaction(UUID walletId, CreateTransactionRequest request, String email) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        UUID groupId = wallet.getGroup().getId();

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
                seedGroupConsensusApprovals(tx, wallet);

                String reason = aiValidationService.getValidationReason(wallet, request.getMerchant(), request.getCategory());
                alertService.alertTransactionDeclined(tx, "AI Warning: " + reason + " — Group approval required");

                auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                        wallet.getGroup().getId(), tx.getId(),
                        "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                                + " on wallet '" + wallet.getName()
                                + "' — Status: PENDING_GROUP_APPROVAL (AI mismatch: " + reason + ")");

                int members = (int) transactionGroupApprovalRepository.countByTransactionId(tx.getId());
                int required = requiredApprovalCount(members);
                String memberSummary = buildMemberEmailSummary(wallet);
                auditLogService.log(AuditAction.TRANSACTION_GROUP_CONSENSUS_REQUESTED, email,
                        wallet.getGroup().getId(), tx.getId(),
                        "Approval requests sent to all " + members + " group member(s); "
                                + required + " approval(s) required (majority). " + memberSummary);

                // Predictive Liquidity Agent: new unsettled obligation — warn if cash will fall short.
                predictiveLiquidityService.checkAndWarn(groupId);

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
            seedGroupConsensusApprovals(tx, wallet);

            alertService.alertTransactionDeclined(tx, "AI validation unavailable — Awaiting manual approval");

            auditLogService.log(AuditAction.TRANSACTION_CREATED, email,
                    wallet.getGroup().getId(), tx.getId(),
                    "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                            + " on wallet '" + wallet.getName() + "' — Status: " + tx.getStatus()
                            + " (AI unavailable; majority group approval required)");

            int members = (int) transactionGroupApprovalRepository.countByTransactionId(tx.getId());
            int required = requiredApprovalCount(members);
            String memberSummary = buildMemberEmailSummary(wallet);
            auditLogService.log(AuditAction.TRANSACTION_GROUP_CONSENSUS_REQUESTED, email,
                    wallet.getGroup().getId(), tx.getId(),
                    "Approval requests sent to all " + members + " group member(s); "
                            + required + " approval(s) required (majority). " + memberSummary);

            // Predictive Liquidity Agent: new unsettled obligation — warn if cash will fall short.
            predictiveLiquidityService.checkAndWarn(groupId);

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

        // Predictive Liquidity Agent: warn the group if this transaction risks a cash shortfall.
        predictiveLiquidityService.checkAndWarn(groupId);

        return toResponse(tx);
    }

    private String buildMemberEmailSummary(Wallet wallet) {
        String emails = subWalletAccessService.eligibleApprovers(wallet).stream()
                .map(User::getEmail)
                .collect(Collectors.joining(", "));
        return "Members: " + emails + ".";
    }

    /**
     * Seeds the approval set with the wallet's eligible approvers. For a root wallet this is
     * every group member; for a sub-wallet only its assigned members plus group admins — so
     * outsiders cannot approve a sub-wallet's transactions.
     */
    private void seedGroupConsensusApprovals(Transaction tx, Wallet wallet) {
        for (User user : subWalletAccessService.eligibleApprovers(wallet)) {
            TransactionGroupApproval row = new TransactionGroupApproval();
            row.setTransaction(tx);
            row.setUser(user);
            transactionGroupApprovalRepository.save(row);
        }
    }

    /** Majority threshold: ⌊memberCount/2⌋ + 1 (e.g. 5 members → 3 approvals). */
    private int requiredApprovalCount(int memberCount) {
        if (memberCount <= 0) {
            return 1;
        }
        return (memberCount / 2) + 1;
    }

    private int requiredApprovalCount(UUID transactionId) {
        return requiredApprovalCount((int) transactionGroupApprovalRepository.countByTransactionId(transactionId));
    }

    private boolean needsGroupConsensus(TransactionStatus status) {
        return status == TransactionStatus.PENDING_GROUP_APPROVAL
                || status == TransactionStatus.PENDING_MANUAL_APPROVAL;
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

        if (needsGroupConsensus(tx.getStatus())) {
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

            int required = requiredApprovalCount(txId);
            long approved = transactionGroupApprovalRepository.countApprovedByTransactionId(txId);

            if (approved >= required) {
                tx.setStatus(TransactionStatus.APPROVED);
                deductFromWalletHierarchy(tx.getWallet(), tx.getAmount());
                transactionRepository.save(tx);
                auditLogService.log(AuditAction.TRANSACTION_APPROVED, email,
                        tx.getWallet().getGroup().getId(), tx.getId(),
                        "Transaction of €" + tx.getAmount() + " at " + tx.getMerchant()
                                + " approved after group majority consensus (" + approved + "/" + required + ")");
                alertService.sendGroupAlert(tx.getWallet().getGroup(),
                        "Transaction Approved: €" + tx.getAmount() + " at " + tx.getMerchant(),
                        "/groups/" + tx.getWallet().getGroup().getId());
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

        if (needsGroupConsensus(tx.getStatus())) {
            if (!subWalletAccessService.canParticipate(email, tx.getWallet())) {
                throw new BadRequestException("Only active members of this wallet can decline this transaction");
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
        if (needsGroupConsensus(tx.getStatus())) {
            groupConsensusRequired = requiredApprovalCount(tx.getId());
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
