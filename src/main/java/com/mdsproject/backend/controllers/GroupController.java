package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.group.ChangeRoleRequest;
import com.mdsproject.backend.dto.group.CreateGroupRequest;
import com.mdsproject.backend.dto.group.GroupResponse;
import com.mdsproject.backend.dto.group.JoinGroupRequest;
import com.mdsproject.backend.services.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(Authentication auth,
                                                     @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.createGroup(auth.getName(), request));
    }

    @PostMapping("/join")
    public ResponseEntity<GroupResponse> joinGroup(Authentication auth,
                                                   @Valid @RequestBody JoinGroupRequest request) {
        return ResponseEntity.ok(groupService.joinGroup(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups(Authentication auth) {
        return ResponseEntity.ok(groupService.getUserGroups(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @PatchMapping("/{groupId}/roles")
    public ResponseEntity<GroupResponse> changeRole(Authentication auth,
                                                    @PathVariable UUID groupId,
                                                    @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(groupService.changeRole(auth.getName(), groupId, request));
    }
}
