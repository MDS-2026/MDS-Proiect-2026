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

import java.util.ArrayList;
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
 *   <li><b>Admin</b> — absolute access to ALL wallets in the group for management
 *       (chat, spending, oversight). Transaction <em>approval</em>, however, is
 *       membership-scoped: an admin may approve a sub-wallet's transactions only if they
 *       are themselves an assigned member of that sub-wallet.</li>
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
     * The set of users eligible to approve / decline a transaction on this wallet.
     * <ul>
     *   <li><b>Root wallet</b> (no parent): every group member — root-wallet membership is
     *       implied by {@link GroupMembership}.</li>
     *   <li><b>Sub-wallet</b>: ONLY the members explicitly assigned to it via
     *       {@link SubWalletMembership}. Group admins are NOT automatically eligible — an
     *       admin can approve a sub-wallet's transactions only if they are an assigned
     *       member, so they cannot approve every sub-wallet by virtue of being admin.</li>
     * </ul>
     */
    public List<User> eligibleApprovers(Wallet wallet) {
        // Use a set keyed by user id to avoid duplicates.
        Set<UUID> seen = new LinkedHashSet<>();
        List<User> result = new ArrayList<>();

        if (wallet.getParentWallet() == null) {
            // Root wallet: membership is implied by group membership.
            for (GroupMembership m : groupMembershipRepository.findByGroupId(wallet.getGroup().getId())) {
                if (seen.add(m.getUser().getId())) {
                    result.add(m.getUser());
                }
            }
        } else {
            // Sub-wallet: only the explicitly assigned members may approve. Admins are
            // included here only if they were assigned as members of this sub-wallet.
            for (SubWalletMembership sm : subWalletMembershipRepository.findByWallet_Id(wallet.getId())) {
                if (seen.add(sm.getUser().getId())) {
                    result.add(sm.getUser());
                }
            }
        }
        return result;
    }

    /** All group admins — they retain absolute oversight/visibility of every wallet. */
    public List<User> groupAdmins(UUID groupId) {
        Set<UUID> seen = new LinkedHashSet<>();
        List<User> result = new ArrayList<>();
        for (GroupMembership m : groupMembershipRepository.findByGroupId(groupId)) {
            if (m.getRole() == Role.ADMIN && seen.add(m.getUser().getId())) {
                result.add(m.getUser());
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
