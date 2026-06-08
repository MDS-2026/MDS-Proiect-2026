package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.wallet.SubWalletMemberResponse;
import com.mdsproject.backend.exceptions.BadRequestException;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.SubWalletMembership;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.repositories.SubWalletMembershipRepository;
import com.mdsproject.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Admin-only management of which Root Wallet members belong to a given Sub-wallet.
 */
@Service
@RequiredArgsConstructor
public class SubWalletMembershipService {

    private final SubWalletMembershipRepository subWalletMembershipRepository;
    private final SubWalletAccessService accessService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public List<SubWalletMemberResponse> assignMembers(UUID walletId, List<UUID> userIds, String actingEmail) {
        Wallet wallet = accessService.requireWallet(walletId);
        UUID groupId = wallet.getGroup().getId();

        accessService.assertIsAdmin(actingEmail, groupId);
        if (!accessService.isSubWallet(wallet)) {
            throw new BadRequestException("Members can only be assigned to a sub-wallet, not the root wallet.");
        }

        for (UUID userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

            // A sub-wallet member must first be a member of the parent group (root wallet).
            if (!accessService.isGroupMember(user.getEmail(), groupId)) {
                throw new BadRequestException(
                        "User " + user.getEmail() + " is not a member of the root wallet's group.");
            }

            if (subWalletMembershipRepository.existsByWallet_IdAndUser_Id(walletId, userId)) {
                continue; // already assigned — idempotent
            }

            SubWalletMembership membership = new SubWalletMembership();
            membership.setWallet(wallet);
            membership.setUser(user);
            subWalletMembershipRepository.save(membership);

            auditLogService.log(AuditAction.SUB_WALLET_MEMBER_ASSIGNED, actingEmail, groupId, walletId,
                    "User '" + user.getEmail() + "' assigned to sub-wallet '" + wallet.getName() + "'");
        }

        return listMembers(walletId);
    }

    @Transactional
    public void removeMember(UUID walletId, UUID userId, String actingEmail) {
        Wallet wallet = accessService.requireWallet(walletId);
        UUID groupId = wallet.getGroup().getId();

        accessService.assertIsAdmin(actingEmail, groupId);
        if (!accessService.isSubWallet(wallet)) {
            throw new BadRequestException("The root wallet has no removable member assignments.");
        }
        if (!subWalletMembershipRepository.existsByWallet_IdAndUser_Id(walletId, userId)) {
            throw new ResourceNotFoundException("User is not assigned to this sub-wallet.");
        }

        subWalletMembershipRepository.deleteByWallet_IdAndUser_Id(walletId, userId);

        User user = userRepository.findById(userId).orElse(null);
        String who = user != null ? user.getEmail() : userId.toString();
        auditLogService.log(AuditAction.SUB_WALLET_MEMBER_REMOVED, actingEmail, groupId, walletId,
                "User '" + who + "' removed from sub-wallet '" + wallet.getName() + "'");
    }

    public List<SubWalletMemberResponse> listMembers(UUID walletId) {
        Wallet wallet = accessService.requireWallet(walletId);
        UUID groupId = wallet.getGroup().getId();

        Set<UUID> seen = new LinkedHashSet<>();
        List<SubWalletMemberResponse> result = new ArrayList<>();

        // Admins always have absolute access to every sub-wallet.
        for (User admin : accessService.eligibleApprovers(wallet)) {
            boolean isAdmin = accessService.isGroupAdmin(admin.getEmail(), groupId);
            if (isAdmin && seen.add(admin.getId())) {
                result.add(new SubWalletMemberResponse(admin.getId(), admin.getEmail(), true));
            }
        }
        for (SubWalletMembership sm : subWalletMembershipRepository.findByWallet_Id(walletId)) {
            if (seen.add(sm.getUser().getId())) {
                result.add(new SubWalletMemberResponse(sm.getUser().getId(), sm.getUser().getEmail(), false));
            }
        }
        return result;
    }
}
