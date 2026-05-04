package com.mdsproject.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendChatMessageRequest {

    @NotBlank
    private String content;
}
