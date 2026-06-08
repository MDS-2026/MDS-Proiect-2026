package com.mdsproject.backend.services;

import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.GroupMembership;
import com.mdsproject.backend.models.SubWalletMembership;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.Role;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import com.mdsproject.backend.repositories.SubWalletMembershipRepository;
import com.mdsproject.backend.repositories.UserRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Central authority for wallet access control.
 *
 * <p>The group itself is the conceptual root; every {@link Wallet} is a membership-scoped
 * sub-wallet. Rules:</p>
 * <ul>
 *   <li><b>Admin</b> — absolute access to ALL wallets in the group.</li>
 *   <li><b>Assigned member</b> — full active access (chat, approvals, spending) ONLY in
 *       the wallets they are assigned to (via {@link SubWalletMembership}).</li>
 *   <li><b>Non-member of a wallet</b> — read-only visibility of that wallet's ledger;
 *       no chat, no approvals.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SubWalletAccessService {

    private final WalletRepository walletRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final SubWalletMembershipRepository subWalletMembershipRepository;
    private final UserRepository userRepository;

    public Wallet requireWallet(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * The group itself is the conceptual root, so every {@link Wallet} entity is a
     * membership-scoped sub-wallet — there is no distinct "root wallet" that bypasses
     * member assignment. Kept as a method so callers/DTOs read clearly.
     */
    public boolean isSubWallet(Wallet wallet) {
        return wallet != null;
    }

    public boolean isGroupAdmin(String email, UUID groupId) {
        return groupMembershipRepository.findByGroupId(groupId).stream()
                .anyMatch(m -> m.getRole() == Role.ADMIN
                        && m.getUser().getEmail().equalsIgnoreCase(email));
    }

    public boolean isGroupMember(String email, UUID groupId) {
        return groupMembershipRepository.existsByUserEmailAndGroupId(email, groupId);
    }

    /**
     * Can the user fully participate in this wallet — i.e. chat, approve and spend?
     * Group admins plus the members explicitly assigned to the wallet.
     */
    public boolean canParticipate(String email, Wallet wallet) {
        UUID groupId = wallet.getGroup().getId();
        if (!isGroupMember(email, groupId)) {
            return false;
        }
        // Every wallet is membership-scoped: only group admins (absolute access) and
        // explicitly assigned members may fully participate.
        return isGroupAdmin(email, groupId)
                || subWalletMembershipRepository.existsByWallet_IdAndUser_Email(wallet.getId(), email);
    }

    /**
     * Can the user VIEW this wallet's ledger? Any group member can — sub-wallet
     * outsiders retain read-only visibility of the transaction history.
     */
    public boolean canViewLedger(String email, Wallet wallet) {
        return isGroupMember(email, wallet.getGroup().getId());
    }

    /**
     * The set of users eligible to approve / decline a transaction on this wallet:
     * the group admins plus the members explicitly assigned to the wallet.
     */
    public List<User> eligibleApprovers(Wallet wallet) {
        List<GroupMembership> groupMembers = groupMembershipRepository.findByGroupId(wallet.getGroup().getId());

        // Every wallet is membership-scoped: approvers are the group admins plus the
        // members explicitly assigned to this wallet.
        // Use a set keyed by user id to merge admins + assigned members without duplicates.
        Set<UUID> seen = new LinkedHashSet<>();
        java.util.List<User> result = new java.util.ArrayList<>();

        for (GroupMembership m : groupMembers) {
            if (m.getRole() == Role.ADMIN && seen.add(m.getUser().getId())) {
                result.add(m.getUser());
            }
        }
        for (SubWalletMembership sm : subWalletMembershipRepository.findByWallet_Id(wallet.getId())) {
            if (seen.add(sm.getUser().getId())) {
                result.add(sm.getUser());
            }
        }
        return result;
    }

    /** Guard helper: throws if the user may not fully participate in the wallet. */
    public void assertCanParticipate(String email, Wallet wallet) {
        if (!canParticipate(email, wallet)) {
            throw new com.mdsproject.backend.exceptions.BadRequestException(
                    "You are not an active member of this sub-wallet.");
        }
    }

    public void assertIsAdmin(String email, UUID groupId) {
        if (!isGroupAdmin(email, groupId)) {
            throw new com.mdsproject.backend.exceptions.BadRequestException(
                    "Only a group admin may perform this action.");
        }
    }
}
