package com.mdsproject.backend.models.enums;

public enum TransactionStatus {
    PENDING,
    /** AI rejected (or AI unavailable); every group member must approve before the transaction is approved. */
    PENDING_GROUP_APPROVAL,
    APPROVED,
    DECLINED
}
