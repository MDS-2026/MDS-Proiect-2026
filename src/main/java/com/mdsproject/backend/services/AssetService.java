package com.mdsproject.backend.services;

import com.mdsproject.backend.dto.asset.AssetResponse;
import com.mdsproject.backend.dto.asset.CreateAssetRequest;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Asset;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.enums.AssetType;
import com.mdsproject.backend.models.enums.AuditAction;
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
    private final AuditLogService auditLogService;
    private final EstimationService estimationService;
    private final GroupMembershipService groupMembershipService;

    public AssetResponse addAsset(UUID groupId, CreateAssetRequest request, String email) {
        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Asset asset = new Asset();
        asset.setGroup(group);
        asset.setType(AssetType.valueOf(request.getType()));
        asset.setProvider(request.getProvider());
        asset.setAmount(request.getAmount());
        asset.setAmountUnit(request.getAmountUnit());

        // Compute estimated EUR for non-cash assets if not provided
        Double estimated = request.getEstimatedEurValue();
        if (asset.getType() != AssetType.CASH && estimated == null) {
            if (request.getAmount() == null) {
                throw new com.mdsproject.backend.exceptions.BadRequestException("Amount is required for non-cash assets");
            }
            estimated = estimationService.estimateEurValue(asset.getType(), asset.getProvider(), request.getAmount());
        }
        asset.setEstimatedEurValue(estimated != null ? estimated : 0.0);
        asset.setExpiryDate(request.getExpiryDate());
        assetRepository.save(asset);

        auditLogService.log(AuditAction.ASSET_ADDED, email, groupId, asset.getId(),
                request.getType() + " asset from " + request.getProvider()
                        + " worth €" + asset.getEstimatedEurValue() + " added");

        // Update member fairness score
        groupMembershipService.incrementFairnessForUserInGroup(email, groupId, asset.getEstimatedEurValue());

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
                asset.getAmount(),
                asset.getAmountUnit(),
                asset.getExpiryDate()
        );
    }
}
