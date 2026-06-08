package com.mdsproject.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubWalletMemberResponse {
    private UUID userId;
    private String email;
    /** True when the user is included by virtue of being a group admin (absolute access). */
    private boolean admin;
}
