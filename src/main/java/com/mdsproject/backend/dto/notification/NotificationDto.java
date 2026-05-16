package com.mdsproject.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private UUID groupId;
    private String groupName;
    private String targetUrl;
}
