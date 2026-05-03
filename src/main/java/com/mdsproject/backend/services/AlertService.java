package com.mdsproject.backend.services;

import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final GroupMembershipRepository groupMembershipRepository;

    /**
     * Send alert to all group members when a transaction is declined due to merchant mismatch.
     * In production, this would send email, SMS, push notifications, or store in-app alerts.
     */
    public void alertTransactionDeclined(Transaction transaction, String reason) {
        FairPayGroup group = transaction.getWallet().getGroup();
        
        // Fetch all group members
        List<User> members = groupMembershipRepository.findByGroupId(group.getId())
                .stream()
                .map(m -> m.getUser())
                .collect(Collectors.toList());

        String alertMessage = String.format(
                "Transaction Declined: €%.2f at '%s' on wallet '%s' — Reason: %s",
                transaction.getAmount(),
                transaction.getMerchant(),
                transaction.getWallet().getName(),
                reason
        );

        log.warn("ALERT for group '{}': {}", group.getName(), alertMessage);

        // Send notification to each member
        // TODO: In production, integrate with notification service (email, SMS, in-app, etc.)
        for (User member : members) {
            sendNotificationToMember(member, alertMessage, transaction, group);
        }
    }

    private void sendNotificationToMember(User member, String alertMessage, Transaction transaction, FairPayGroup group) {
        log.info("Sending alert to user '{}': {}", member.getEmail(), alertMessage);
        // TODO: Implement actual notification delivery
        // - Send email via EmailService
        // - Send push notification via PushService
        // - Store as in-app Notification entity in database
    }
}
