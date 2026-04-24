package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.transaction.CreateTransactionRequest;
import com.mdsproject.backend.dto.transaction.TransactionResponse;
import com.mdsproject.backend.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/api/wallets/{walletId}/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@PathVariable UUID walletId,
                                                                 @Valid @RequestBody CreateTransactionRequest request,
                                                                 Authentication auth) {
        return ResponseEntity.ok(transactionService.createTransaction(walletId, request, auth.getName()));
    }

    @GetMapping("/api/wallets/{walletId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getWalletTransactions(@PathVariable UUID walletId) {
        return ResponseEntity.ok(transactionService.getWalletTransactions(walletId));
    }

    @GetMapping("/api/groups/{groupId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getGroupTransactions(@PathVariable UUID groupId) {
        return ResponseEntity.ok(transactionService.getGroupTransactions(groupId));
    }

    @PatchMapping("/api/transactions/{id}/approve")
    public ResponseEntity<TransactionResponse> approve(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(transactionService.approveTransaction(id, auth.getName()));
    }

    @PatchMapping("/api/transactions/{id}/decline")
    public ResponseEntity<TransactionResponse> decline(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(transactionService.declineTransaction(id, auth.getName()));
    }
}
