package com.mdsproject.backend.dto.wallet;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AssignSubWalletMembersRequest {
    /** Root-wallet member user ids to assign to this sub-wallet. */
    @NotEmpty
    private List<UUID> userIds;
}
