package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.wallet.AssignSubWalletMembersRequest;
import com.mdsproject.backend.dto.wallet.SubWalletAccessResponse;
import com.mdsproject.backend.dto.wallet.SubWalletMemberResponse;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.services.SubWalletAccessService;
import com.mdsproject.backend.services.SubWalletMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sub-wallet membership and access endpoints (operate on a wallet directly, independent of
 * the group path used by {@link WalletController}).
 */
@RestController
@RequestMapping("/api/wallets/{walletId}")
@RequiredArgsConstructor
public class SubWalletController {

    private final SubWalletMembershipService membershipService;
    private final SubWalletAccessService accessService;

    /** Admin assigns root-wallet members to this sub-wallet. */
    @PostMapping("/members")
    public ResponseEntity<List<SubWalletMemberResponse>> assignMembers(
            @PathVariable UUID walletId,
            @Valid @RequestBody AssignSubWalletMembersRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                membershipService.assignMembers(walletId, request.getUserIds(), auth.getName()));
    }

    /** Admin removes a member from this sub-wallet. */
    @DeleteMapping("/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID walletId,
                                             @PathVariable UUID userId,
                                             Authentication auth) {
        membershipService.removeMember(walletId, userId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    /** Lists the active members of this sub-wallet (admins included). */
    @GetMapping("/members")
    public ResponseEntity<List<SubWalletMemberResponse>> listMembers(@PathVariable UUID walletId) {
        return ResponseEntity.ok(membershipService.listMembers(walletId));
    }

    /** Returns the calling user's access level for this wallet. */
    @GetMapping("/access")
    public ResponseEntity<SubWalletAccessResponse> getAccess(@PathVariable UUID walletId,
                                                             Authentication auth) {
        String email = auth.getName();
        Wallet wallet = accessService.requireWallet(walletId);
        return ResponseEntity.ok(new SubWalletAccessResponse(
                walletId,
                accessService.isSubWallet(wallet),
                accessService.isGroupAdmin(email, wallet.getGroup().getId()),
                accessService.canParticipate(email, wallet),
                accessService.canViewLedger(email, wallet)
        ));
    }
}
