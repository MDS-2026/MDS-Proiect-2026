package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.chat.ChatMessageDTO;
import com.mdsproject.backend.models.ChatMessage;
import com.mdsproject.backend.repositories.ChatMessageRepository;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups/{groupId}/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatMessageRepository chatMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    /**
     * GET /api/groups/{groupId}/chat
     * Returns full chat history for the group (oldest first).
     * Only accessible by group members.
     */
    @GetMapping
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable UUID groupId,
            Authentication auth) {

        String email = auth.getName();

        // Verify caller is a group member
        if (!groupMembershipRepository.existsByUserEmailAndGroupId(email, groupId)) {
            return ResponseEntity.status(403).build();
        }

        List<ChatMessageDTO> history = chatMessageRepository
                .findByGroupIdOrderByCreatedAtAsc(groupId)
                .stream()
                .map(m -> new ChatMessageDTO(
                        m.getId(),
                        m.getGroupId(),
                        m.getSenderEmail(),
                        m.getContent(),
                        m.getCreatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
}
