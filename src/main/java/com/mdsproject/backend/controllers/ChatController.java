package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.chat.ChatMessageDTO;
import com.mdsproject.backend.dto.chat.SendChatMessageRequest;
import com.mdsproject.backend.models.ChatMessage;
import com.mdsproject.backend.repositories.ChatMessageRepository;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles messages sent to /app/chat/{groupId}
     * Validates the sender is a member, saves the message,
     * then broadcasts to /topic/chat/{groupId}
     */
    @MessageMapping("/chat/{groupId}")
    public void sendMessage(@DestinationVariable UUID groupId,
                            @Payload SendChatMessageRequest request,
                            Principal principal) {

        if (principal == null) {
            return; // Unauthenticated – silently drop
        }

        String senderEmail = principal.getName();

        // Verify sender is a member of the group
        boolean isMember = groupMembershipRepository.existsByUserEmailAndGroupId(senderEmail, groupId);
        if (!isMember) {
            return; // Not a member – silently drop
        }

        // Persist the message
        ChatMessage msg = new ChatMessage();
        msg.setGroupId(groupId);
        msg.setSenderEmail(senderEmail);
        msg.setContent(request.getContent().trim());
        ChatMessage saved = chatMessageRepository.save(msg);

        // Broadcast to all subscribers of this group's chat topic
        ChatMessageDTO dto = new ChatMessageDTO(
                saved.getId(),
                saved.getGroupId(),
                saved.getSenderEmail(),
                saved.getContent(),
                saved.getCreatedAt()
        );

        messagingTemplate.convertAndSend("/topic/chat/" + groupId, dto);
    }
}
