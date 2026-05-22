package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.notification.NotificationDto;
import com.mdsproject.backend.models.Notification;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.repositories.NotificationRepository;
import com.mdsproject.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getUserNotifications(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<NotificationDto> dtos = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(n -> new NotificationDto(
                        n.getId(),
                        n.getMessage(),
                        n.isRead(),
                        n.getCreatedAt(),
                        n.getGroup() != null ? n.getGroup().getId() : null,
                        n.getGroup() != null ? n.getGroup().getName() : null,
                        n.getTargetUrl()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }
}
