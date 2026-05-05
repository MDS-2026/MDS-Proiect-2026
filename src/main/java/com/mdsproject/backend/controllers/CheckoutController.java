package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.transaction.PaymentSplitPreviewResponse;
import com.mdsproject.backend.services.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @GetMapping("/preview")
    public ResponseEntity<PaymentSplitPreviewResponse> getSplitPreview(
            @RequestParam UUID walletId,
            @RequestParam Double amount) {
        
        PaymentSplitPreviewResponse response = checkoutService.getPreview(walletId, amount);
        return ResponseEntity.ok(response);
    }
}