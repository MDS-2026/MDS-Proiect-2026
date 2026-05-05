package com.mdsproject.backend.dto.transaction; 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSplitPreviewResponse {
    private Double totalAmount;
    private Double cashAmount;
    private Double milesAmount;
    private Double voucherAmount;
    private String currency; 
}