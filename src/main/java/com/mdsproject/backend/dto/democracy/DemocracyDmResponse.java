package com.mdsproject.backend.dto.democracy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemocracyDmResponse {
    private boolean pending;
    private UUID sessionId;
    private String goalText;
}
