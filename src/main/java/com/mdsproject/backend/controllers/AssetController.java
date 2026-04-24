package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.asset.AssetResponse;
import com.mdsproject.backend.dto.asset.CreateAssetRequest;
import com.mdsproject.backend.services.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> addAsset(@PathVariable UUID groupId,
                                                  @Valid @RequestBody CreateAssetRequest request) {
        return ResponseEntity.ok(assetService.addAsset(groupId, request));
    }

    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAssets(@PathVariable UUID groupId) {
        return ResponseEntity.ok(assetService.getGroupAssets(groupId));
    }
}
