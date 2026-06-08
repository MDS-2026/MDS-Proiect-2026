package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.chat.ChatMessageDTO;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.repositories.ChatMessageRepository;
import com.mdsproject.backend.services.SubWalletAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST history for an isolated sub-wallet chat. Only active members (and admins) may read it;
 * outsiders — who retain read-only ledger visibility elsewhere — get 403 here.
 */
@RestController
@RequestMapping("/api/wallets/{walletId}/chat")
@RequiredArgsConstructor
public class SubWalletChatRestController {

    private final ChatMessageRepository chatMessageRepository;
    private final SubWalletAccessService accessService;

    @GetMapping
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable UUID walletId,
                                                               Authentication auth) {
        Wallet wallet = accessService.requireWallet(walletId);

        if (!accessService.canParticipate(auth.getName(), wallet)) {
            return ResponseEntity.status(403).build();
        }

        List<ChatMessageDTO> history = chatMessageRepository
                .findByGroupIdOrderByCreatedAtAsc(walletId)
                .stream()
                .map(m -> new ChatMessageDTO(
                        m.getId(),
                        m.getGroupId(),
                        m.getSenderEmail(),
                        m.getContent(),
                        m.getCreatedAt(),
                        m.getMessageType(),
                        m.getMetadata()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }
}
