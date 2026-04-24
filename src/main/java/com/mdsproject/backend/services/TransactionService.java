package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.transaction.CreateTransactionRequest;
import com.mdsproject.backend.dto.transaction.TransactionResponse;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.Wallet;
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

    @Transactional
    public TransactionResponse createTransaction(UUID walletId, CreateTransactionRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setMerchant(request.getMerchant());
        tx.setCategory(request.getCategory());
        tx.setWallet(wallet);

        // Auto-approve if amount is within threshold
        if (request.getAmount() <= wallet.getAutoApproveThreshold()) {
            tx.setStatus(TransactionStatus.APPROVED);
        } else {
            tx.setStatus(TransactionStatus.PENDING);
        }

        transactionRepository.save(tx);
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

    @Transactional
    public TransactionResponse approveTransaction(UUID txId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        tx.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(tx);
        return toResponse(tx);
    }

    @Transactional
    public TransactionResponse declineTransaction(UUID txId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        tx.setStatus(TransactionStatus.DECLINED);
        transactionRepository.save(tx);
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
