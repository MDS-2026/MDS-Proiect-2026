package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.chat.ChatMessageDTO;
import com.mdsproject.backend.dto.chat.SendChatMessageRequest;
import com.mdsproject.backend.models.ChatMessage;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.repositories.ChatMessageRepository;
import com.mdsproject.backend.repositories.UserRepository;
import com.mdsproject.backend.services.AlertService;
import com.mdsproject.backend.services.SubWalletAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Isolated private chat for a single sub-wallet.
 *
 * <p>Messages are persisted in the shared {@link ChatMessage} table keyed by the sub-wallet's
 * id (reusing the channel column), and broadcast on a dedicated topic. Only the sub-wallet's
 * active members (and group admins) may send; outsiders cannot read or participate.</p>
 */
@Controller
@RequiredArgsConstructor
public class SubWalletChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SubWalletAccessService accessService;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles messages sent to /app/wallet-chat/{walletId}, validates the sender is an active
     * member of the sub-wallet, persists the message and broadcasts to
     * /topic/wallet-chat/{walletId}.
     */
    @MessageMapping("/wallet-chat/{walletId}")
    public void sendMessage(@DestinationVariable UUID walletId,
                            @Payload SendChatMessageRequest request,
                            Principal principal) {

        if (principal == null) {
            return; // Unauthenticated – silently drop
        }

        String senderEmail = principal.getName();
        Wallet wallet = accessService.requireWallet(walletId);

        // Only active sub-wallet members (and admins) may chat here.
        if (!accessService.canParticipate(senderEmail, wallet)) {
            return; // Outsider – silently drop
        }

        ChatMessage msg = new ChatMessage();
        msg.setGroupId(walletId); // channel key = sub-wallet id
        msg.setSenderEmail(senderEmail);
        msg.setContent(request.getContent().trim());
        ChatMessage saved = chatMessageRepository.save(msg);

        ChatMessageDTO dto = new ChatMessageDTO(
                saved.getId(),
                saved.getGroupId(),
                saved.getSenderEmail(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getMessageType(),
                saved.getMetadata());

        messagingTemplate.convertAndSend("/topic/wallet-chat/" + walletId, dto);

        // Notify only the sub-wallet's members — never the whole root-wallet group.
        User sender = userRepository.findByEmail(senderEmail).orElse(null);
        if (sender != null) {
            String text = "New message from " + senderEmail + " in sub-wallet '" + wallet.getName() + "'";
            alertService.notifyUsers(
                    accessService.eligibleApprovers(wallet),
                    sender,
                    wallet.getGroup(),
                    text,
                    "/wallets/" + walletId + "?tab=chat");
        }
    }
}
