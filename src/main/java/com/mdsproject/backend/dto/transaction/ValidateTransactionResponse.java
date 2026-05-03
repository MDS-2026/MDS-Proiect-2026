package com.mdsproject.backend.dto.transaction;

public record ValidateTransactionResponse(boolean valid, String reason) {
}
