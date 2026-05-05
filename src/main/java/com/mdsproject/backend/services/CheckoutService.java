package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.transaction.PaymentSplitPreviewResponse;
import com.mdsproject.backend.models.Asset;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AssetType;
import com.mdsproject.backend.repositories.AssetRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final AssetRepository assetRepository;
    private final WalletRepository walletRepository;

    public PaymentSplitPreviewResponse getPreview(UUID walletId, Double amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        List<Asset> allAssets = assetRepository.findByGroupId(wallet.getGroup().getId());
        LocalDate today = LocalDate.now();

        List<Asset> validAssets = allAssets.stream()
                .filter(a -> a.getExpiryDate() == null || !a.getExpiryDate().isBefore(today))
                .toList();

        double remainingToPay = amount;
        double usedVouchers = 0.0;
        double usedMiles = 0.0;

        List<Asset> vouchers = validAssets.stream()
                .filter(a -> a.getType() == AssetType.VOUCHER)
                .sorted(Comparator.comparing(Asset::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (Asset v : vouchers) {
            if (remainingToPay <= 0) break;
            double value = v.getEstimatedEurValue();
            double take = Math.min(remainingToPay, value);
            usedVouchers += take;
            remainingToPay -= take;
        }

        List<Asset> miles = validAssets.stream()
                .filter(a -> a.getType() == AssetType.MILES)
                .sorted(Comparator.comparing(Asset::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (Asset m : miles) {
            if (remainingToPay <= 0) break;
            double value = m.getEstimatedEurValue();
            double take = Math.min(remainingToPay, value);
            usedMiles += take;
            remainingToPay -= take;
        }

        double usedCash = remainingToPay;

        return new PaymentSplitPreviewResponse(amount, usedCash, usedMiles, usedVouchers, "EUR");
    }
}