package com.mdsproject.backend.repositories;

import com.mdsproject.backend.models.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByGroupId(UUID groupId);
}
