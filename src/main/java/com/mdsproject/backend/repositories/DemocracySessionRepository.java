package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.DemocracySession;
import com.mdsproject.backend.models.enums.DemocracySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DemocracySessionRepository extends JpaRepository<DemocracySession, UUID> {

    Optional<DemocracySession> findFirstByGroupIdAndStatusOrderByCreatedAtDesc(
            UUID groupId, DemocracySessionStatus status);

    Optional<DemocracySession> findFirstByGroupIdAndStatusInOrderByCreatedAtDesc(
            UUID groupId, java.util.List<DemocracySessionStatus> statuses);

    boolean existsByGroupIdAndStatusIn(UUID groupId, java.util.List<DemocracySessionStatus> statuses);
}
