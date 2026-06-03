package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.group.*;
import com.mdsproject.backend.exceptions.BadRequestException;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.GroupMembership;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.VirtualCard;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.models.enums.Role;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import com.mdsproject.backend.repositories.UserRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import com.mdsproject.backend.repositories.VirtualCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final FairPayGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final WalletRepository walletRepository;
    private final VirtualCardRepository virtualCardRepository;
        private final com.mdsproject.backend.repositories.AssetRepository assetRepository;

    @Transactional
    public GroupResponse createGroup(String email, CreateGroupRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FairPayGroup group = new FairPayGroup();
        group.setName(request.getName());
        group.setInviteCode(generateInviteCode());
        groupRepository.save(group);

        GroupMembership membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(Role.ADMIN);
        membershipRepository.save(membership);

        // Auto-create root wallet for the group
        Wallet rootWallet = new Wallet();
        rootWallet.setName(group.getName());
        rootWallet.setPurpose("Root wallet for group");
        rootWallet.setBudgetLimit(0.0);
        rootWallet.setAutoApproveThreshold(0.0);
        rootWallet.setGroup(group);
        walletRepository.save(rootWallet);

        // Generate virtual card for root wallet
        VirtualCard card = new VirtualCard();
        card.setCardNumber(generateCardNumber());
        card.setCvv(String.format("%03d", new java.util.Random().nextInt(1000)));
        card.setExpiryDate(java.time.LocalDate.now().plusYears(3));
        card.setActive(true);
        card.setWallet(rootWallet);
        virtualCardRepository.save(card);

        auditLogService.log(AuditAction.GROUP_CREATED, email, group.getId(), group.getId(),
                "Group '" + group.getName() + "' created");

        auditLogService.log(AuditAction.WALLET_CREATED, email, group.getId(), rootWallet.getId(),
                "Root wallet '" + rootWallet.getName() + "' auto-created for group");

        return toGroupResponse(group);
    }

    @Transactional
    public GroupResponse joinGroup(String email, JoinGroupRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FairPayGroup group = groupRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invite code"));

        if (membershipRepository.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new BadRequestException("Already a member of this group");
        }

        GroupMembership membership = new GroupMembership();
        membership.setUser(user);
        membership.setGroup(group);
        membership.setRole(Role.MEMBER);
        membershipRepository.save(membership);

        auditLogService.log(AuditAction.GROUP_JOINED, email, group.getId(), user.getId(),
                email + " joined the group");

        return toGroupResponse(group);
    }

    @Transactional
    public GroupResponse changeRole(String email, UUID groupId, ChangeRoleRequest request) {
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        // Verify requester is ADMIN
        GroupMembership requesterMembership = membershipRepository
                .findByUserIdAndGroupId(requester.getId(), groupId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this group"));

        if (requesterMembership.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only admins can change roles");
        }

        // Find target membership
        GroupMembership targetMembership = membershipRepository
                .findByUserIdAndGroupId(request.getUserId(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this group"));

        Role newRole = Role.valueOf(request.getRole().toUpperCase());
        Role oldRole = targetMembership.getRole();
        targetMembership.setRole(newRole);
        membershipRepository.save(targetMembership);

        auditLogService.log(AuditAction.ROLE_CHANGED, email, groupId, request.getUserId(),
                "Role changed from " + oldRole + " to " + newRole + " for user " + targetMembership.getUser().getEmail());

        return toGroupResponse(group);
    }

    public List<GroupResponse> getUserGroups(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return membershipRepository.findByUserId(user.getId()).stream()
                .map(m -> toGroupResponse(m.getGroup()))
                .collect(Collectors.toList());
    }

    public GroupResponse getGroupById(UUID groupId) {
        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        return toGroupResponse(group);
    }

    private GroupResponse toGroupResponse(FairPayGroup group) {
        List<GroupMembership> memberships = membershipRepository.findByGroupId(group.getId());
        List<MemberResponse> members = memberships.stream()
                .map(m -> new MemberResponse(
                        m.getUser().getId(),
                        m.getUser().getEmail(),
                        m.getRole().name(),
                        m.getFairnessScore()
                ))
                .collect(Collectors.toList());

        // Compute total purchasing power from assets
        double total = assetRepository.findByGroupId(group.getId()).stream()
                .mapToDouble(a -> a.getEstimatedEurValue() == null ? 0.0 : a.getEstimatedEurValue())
                .sum();

        return new GroupResponse(group.getId(), group.getName(), group.getInviteCode(), total, members);
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateCardNumber() {
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder("4");
        for (int i = 1; i < 16; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}
