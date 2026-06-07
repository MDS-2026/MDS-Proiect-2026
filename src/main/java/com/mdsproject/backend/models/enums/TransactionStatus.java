package com.mdsproject.backend.models.enums;

public enum TransactionStatus {
    PENDING,
    /** AI rejected or unavailable; a majority of group members (⌊n/2⌋+1) must approve before the transaction is approved. */
    PENDING_GROUP_APPROVAL,
    APPROVED,
    DECLINED,
    PENDING_MANUAL_APPROVAL
}
