package com.mdsproject.backend.services;

import com.mdsproject.backend.models.GroupMembership;
import com.mdsproject.backend.models.User;
import com.mdsproject.backend.repositories.GroupMembershipRepository;
import com.mdsproject.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMembershipService {

    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional
    public void incrementFairnessForUserInGroup(String email, UUID groupId, Double assetEurValue) {
        if (assetEurValue == null || assetEurValue <= 0) return;

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        GroupMembership membership = membershipRepository.findByUserIdAndGroupId(user.getId(), groupId).orElse(null);
        if (membership == null) return;

        // Increase fairness by a bounded amount proportional to contribution
        double increment = Math.min(5.0, assetEurValue / 100.0);
        membership.setFairnessScore(membership.getFairnessScore() + increment);
        membershipRepository.save(membership);
    }
}
