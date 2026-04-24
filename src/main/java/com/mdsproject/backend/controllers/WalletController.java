package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.wallet.CreateWalletRequest;
import com.mdsproject.backend.dto.wallet.WalletResponse;
import com.mdsproject.backend.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
                                                       @Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.ok(walletService.createWallet(groupId, request));
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getWallets(@PathVariable UUID groupId) {
        return ResponseEntity.ok(walletService.getGroupWallets(groupId));
    }
}
