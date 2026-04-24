package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.wallet.CreateWalletRequest;
import com.mdsproject.backend.dto.wallet.VirtualCardResponse;
import com.mdsproject.backend.dto.wallet.WalletResponse;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.VirtualCard;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import com.mdsproject.backend.repositories.VirtualCardRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mdsproject.backend.dto.wallet.WalletTreeResponse;
import com.mdsproject.backend.models.enums.TransactionStatus;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final FairPayGroupRepository groupRepository;
    private final VirtualCardRepository virtualCardRepository;
    private final AuditLogService auditLogService;
    private final Random random = new Random();

    @Transactional
    public WalletResponse createWallet(UUID groupId, CreateWalletRequest request, String email) {
        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Wallet wallet = new Wallet();
        wallet.setName(request.getName());
        wallet.setPurpose(request.getPurpose());
        wallet.setBudgetLimit(request.getBudgetLimit());
        wallet.setAutoApproveThreshold(request.getAutoApproveThreshold());
        wallet.setGroup(group);

        if (request.getParentWalletId() != null) {
            Wallet parent = walletRepository.findById(request.getParentWalletId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent wallet not found"));
            wallet.setParentWallet(parent);
        }

        walletRepository.save(wallet);

        // Generate virtual card for this wallet
        VirtualCard card = generateVirtualCard(wallet);
        virtualCardRepository.save(card);

        auditLogService.log(AuditAction.WALLET_CREATED, email, groupId, wallet.getId(),
                "Wallet '" + wallet.getName() + "' created with budget €" + wallet.getBudgetLimit());

        auditLogService.log(AuditAction.VIRTUAL_CARD_GENERATED, email, groupId, wallet.getId(),
                "Virtual card generated for wallet '" + wallet.getName() + "'");

        return toResponse(wallet);
    }

    public List<WalletResponse> getGroupWallets(UUID groupId) {
        return walletRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<WalletTreeResponse> getWalletTree(UUID groupId) {
        List<Wallet> allWallets = walletRepository.findByGroupId(groupId);
        // Find root wallets (those with no parent)
        return allWallets.stream()
                .filter(w -> w.getParentWallet() == null)
                .map(this::toTreeResponse)
                .collect(Collectors.toList());
    }

    private WalletTreeResponse toTreeResponse(Wallet wallet) {
        double spent = wallet.getTransactions().stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .mapToDouble(t -> t.getAmount())
                .sum();

        List<WalletTreeResponse> children = wallet.getSubWallets().stream()
                .map(this::toTreeResponse)
                .collect(Collectors.toList());

        return new WalletTreeResponse(
                wallet.getId(),
                wallet.getName(),
                wallet.getPurpose(),
                wallet.getBudgetLimit(),
                wallet.getAutoApproveThreshold(),
                spent,
                children
        );
    }

    public VirtualCardResponse getVirtualCard(UUID walletId) {
        VirtualCard card = virtualCardRepository.findByWalletId(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Virtual card not found for this wallet"));
        return toCardResponse(card);
    }

    public List<VirtualCardResponse> getGroupVirtualCards(UUID groupId) {
        List<Wallet> wallets = walletRepository.findByGroupId(groupId);
        return wallets.stream()
                .map(w -> virtualCardRepository.findByWalletId(w.getId()).orElse(null))
                .filter(c -> c != null)
                .map(this::toCardResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponse updateAutoApproveThreshold(UUID walletId, Double threshold, String email) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Double oldThreshold = wallet.getAutoApproveThreshold();
        wallet.setAutoApproveThreshold(threshold);
        walletRepository.save(wallet);

        auditLogService.log(AuditAction.AUTO_APPROVE_THRESHOLD_CHANGED, email,
                wallet.getGroup().getId(), walletId,
                "Auto-approve threshold changed from €" + oldThreshold + " to €" + threshold
                        + " for wallet '" + wallet.getName() + "'");

        return toResponse(wallet);
    }

    private VirtualCard generateVirtualCard(Wallet wallet) {
        VirtualCard card = new VirtualCard();
        card.setCardNumber(generateCardNumber());
        card.setCvv(String.format("%03d", random.nextInt(1000)));
        card.setExpiryDate(LocalDate.now().plusYears(3));
        card.setActive(true);
        card.setWallet(wallet);
        return card;
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        // Start with 4 (Visa-like prefix)
        sb.append("4");
        for (int i = 1; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() < 16) return cardNumber;
        return "•••• •••• •••• " + cardNumber.substring(12);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getName(),
                wallet.getPurpose(),
                wallet.getBudgetLimit(),
                wallet.getAutoApproveThreshold(),
                wallet.getParentWallet() != null ? wallet.getParentWallet().getId() : null,
                wallet.getGroup().getId()
        );
    }

    private VirtualCardResponse toCardResponse(VirtualCard card) {
        return new VirtualCardResponse(
                card.getId(),
                card.getCardNumber(),
                maskCardNumber(card.getCardNumber()),
                card.getCvv(),
                card.getExpiryDate(),
                card.getActive(),
                card.getWallet().getId(),
                card.getWallet().getName()
        );
    }
}
