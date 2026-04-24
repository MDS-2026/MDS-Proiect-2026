package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.wallet.CreateWalletRequest;
import com.mdsproject.backend.dto.wallet.WalletResponse;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final FairPayGroupRepository groupRepository;

    public WalletResponse createWallet(UUID groupId, CreateWalletRequest request) {
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
        return toResponse(wallet);
    }

    public List<WalletResponse> getGroupWallets(UUID groupId) {
        return walletRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
}
