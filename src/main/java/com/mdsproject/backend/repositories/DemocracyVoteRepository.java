package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.DemocracyVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemocracyVoteRepository extends JpaRepository<DemocracyVote, UUID> {

    List<DemocracyVote> findBySessionId(UUID sessionId);

    Optional<DemocracyVote> findBySessionIdAndVoterEmail(UUID sessionId, String voterEmail);

    boolean existsBySessionIdAndVoterEmail(UUID sessionId, String voterEmail);

    long countBySessionIdAndOptionIndex(UUID sessionId, int optionIndex);
}
