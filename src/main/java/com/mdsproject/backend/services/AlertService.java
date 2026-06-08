package com.mdsproject.backend.services;

import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.Notification;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import com.mdsproject.backend.repositories.NotificationRepository;
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
    private final NotificationRepository notificationRepository;

    /**
     * Send alert to all group members when a transaction is declined due to merchant mismatch.
     * In production, this would send email, SMS, push notifications, or store in-app alerts.
     */
    public void alertTransactionDeclined(Transaction transaction, String reason) {
        FairPayGroup group = transaction.getWallet().getGroup();
        String alertMessage = String.format(
                "Transaction Declined: €%.2f at '%s' on wallet '%s' — Reason: %s",
                transaction.getAmount(), transaction.getMerchant(), transaction.getWallet().getName(), reason
        );
        sendGroupAlert(group, alertMessage, "/groups/" + group.getId());
    }

    public void sendGroupAlert(FairPayGroup group, String message, String targetUrl) {
        List<User> members = groupMembershipRepository.findByGroupId(group.getId())
                .stream().map(m -> m.getUser()).collect(Collectors.toList());
        for (User member : members) {
            sendNotificationToMember(member, message, group, targetUrl);
        }
    }

    public void sendChatNotification(FairPayGroup group, User sender, String text) {
        List<User> members = groupMembershipRepository.findByGroupId(group.getId())
                .stream().map(m -> m.getUser()).collect(Collectors.toList());
        String msg = "New message from " + sender.getEmail() + " in " + group.getName();
        for (User member : members) {
            if (!member.getId().equals(sender.getId())) {
                sendNotificationToMember(member, msg, group, "/groups/" + group.getId() + "?tab=chat");
            }
        }
    }

    /**
     * Notifies only the given recipients (e.g. the members of an isolated sub-wallet chat),
     * never leaking the message to the rest of the root-wallet group.
     */
    public void notifyUsers(List<User> recipients, User sender, FairPayGroup group,
                            String message, String targetUrl) {
        for (User member : recipients) {
            if (sender == null || !member.getId().equals(sender.getId())) {
                sendNotificationToMember(member, message, group, targetUrl);
            }
        }
    }

    private void sendNotificationToMember(User member, String alertMessage, FairPayGroup group, String targetUrl) {
        Notification notification = new Notification();
        notification.setUser(member);
        notification.setGroup(group);
        notification.setMessage(alertMessage);
        notification.setTargetUrl(targetUrl);
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}
