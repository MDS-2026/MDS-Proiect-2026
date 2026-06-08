package com.mdsproject.backend.controllers;

import com.mdsproject.backend.dto.democracy.DemocracyDmResponse;
import com.mdsproject.backend.dto.democracy.SubmitConstraintsRequest;
import com.mdsproject.backend.dto.democracy.VoteRequest;
import com.mdsproject.backend.services.DemocracyAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/democracy")
@RequiredArgsConstructor
public class DemocracyController {

    private final DemocracyAgentService democracyAgentService;

    /**
     * GET /api/groups/{groupId}/democracy/dm
     * Returns whether the current user has a pending DM from the Democracy Agent.
     */
    @GetMapping("/dm")
    public ResponseEntity<DemocracyDmResponse> getPendingDm(
            @PathVariable UUID groupId,
            Authentication auth) {
        return ResponseEntity.ok(democracyAgentService.getPendingDm(groupId, auth.getName()));
    }

    /**
     * POST /api/groups/{groupId}/democracy/constraints
     * Submit the current user's preferences for an active session.
     */
    @PostMapping("/constraints")
    public ResponseEntity<Void> submitConstraints(
            @PathVariable UUID groupId,
            @RequestBody SubmitConstraintsRequest request,
            Authentication auth) {
        democracyAgentService.submitConstraints(groupId, auth.getName(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/groups/{groupId}/democracy/vote
     * Cast a vote for one of the 3 proposed options.
     */
    @PostMapping("/vote")
    public ResponseEntity<Map<String, Object>> vote(
            @PathVariable UUID groupId,
            @RequestBody VoteRequest request,
            Authentication auth) {
        Map<String, Object> voteState = democracyAgentService.castVote(
                groupId, auth.getName(), request.getSessionId(), request.getOptionIndex());
        return ResponseEntity.ok(voteState);
    }

    /**
     * GET /api/groups/{groupId}/democracy/session
     * Get the active session state including current vote counts.
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getSessionState(
            @PathVariable UUID groupId,
            Authentication auth) {
        return ResponseEntity.ok(democracyAgentService.getSessionState(groupId, auth.getName()));
    }
}
