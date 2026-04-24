package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {
    List<GroupMembership> findByUserId(UUID userId);
    List<GroupMembership> findByGroupId(UUID groupId);
    Optional<GroupMembership> findByUserIdAndGroupId(UUID userId, UUID groupId);
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
}
