package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.wallet.CreateWalletRequest;
import com.mdsproject.backend.dto.wallet.VirtualCardResponse;
import com.mdsproject.backend.dto.wallet.WalletResponse;
import com.mdsproject.backend.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@PathVariable UUID groupId,
                                                       @Valid @RequestBody CreateWalletRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(walletService.createWallet(groupId, request, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getWallets(@PathVariable UUID groupId) {
        return ResponseEntity.ok(walletService.getGroupWallets(groupId));
    }

    @GetMapping("/tree")
    public ResponseEntity<List<com.mdsproject.backend.dto.wallet.WalletTreeResponse>> getWalletTree(@PathVariable UUID groupId) {
        return ResponseEntity.ok(walletService.getWalletTree(groupId));
    }

    @GetMapping("/cards")
    public ResponseEntity<List<VirtualCardResponse>> getVirtualCards(@PathVariable UUID groupId) {
        return ResponseEntity.ok(walletService.getGroupVirtualCards(groupId));
    }

    @GetMapping("/{walletId}/card")
    public ResponseEntity<VirtualCardResponse> getVirtualCard(@PathVariable UUID groupId,
                                                               @PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getVirtualCard(walletId));
    }

    @PatchMapping("/{walletId}/threshold")
    public ResponseEntity<WalletResponse> updateThreshold(@PathVariable UUID groupId,
                                                           @PathVariable UUID walletId,
                                                           @RequestParam Double threshold,
                                                           Authentication auth) {
        return ResponseEntity.ok(walletService.updateAutoApproveThreshold(walletId, threshold, auth.getName()));
    }
}
