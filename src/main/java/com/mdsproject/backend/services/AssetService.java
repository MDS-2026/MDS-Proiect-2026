package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.asset.AssetResponse;
import com.mdsproject.backend.dto.asset.CreateAssetRequest;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Asset;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.enums.AssetType;
import com.mdsproject.backend.repositories.AssetRepository;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final FairPayGroupRepository groupRepository;

    public AssetResponse addAsset(UUID groupId, CreateAssetRequest request) {
        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Asset asset = new Asset();
        asset.setGroup(group);
        asset.setType(AssetType.valueOf(request.getType()));
        asset.setProvider(request.getProvider());
        asset.setEstimatedEurValue(request.getEstimatedEurValue());
        asset.setExpiryDate(request.getExpiryDate());
        assetRepository.save(asset);

        return toResponse(asset);
    }

    public List<AssetResponse> getGroupAssets(UUID groupId) {
        return assetRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getType().name(),
                asset.getProvider(),
                asset.getEstimatedEurValue(),
                asset.getExpiryDate()
        );
    }
}
