package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.MemberConstraint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberConstraintRepository extends JpaRepository<MemberConstraint, UUID> {

    List<MemberConstraint> findBySessionId(UUID sessionId);

    Optional<MemberConstraint> findBySessionIdAndMemberEmail(UUID sessionId, String memberEmail);

    boolean existsBySessionIdAndMemberEmail(UUID sessionId, String memberEmail);

    long countBySessionId(UUID sessionId);
}
