package com.mdsproject.backend.dto.democracy;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class VoteRequest {
    private UUID sessionId;
    private int optionIndex;
}
