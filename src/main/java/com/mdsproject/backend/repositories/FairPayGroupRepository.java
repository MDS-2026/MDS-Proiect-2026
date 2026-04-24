package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.FairPayGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FairPayGroupRepository extends JpaRepository<FairPayGroup, UUID> {
    Optional<FairPayGroup> findByInviteCode(String inviteCode);
}
