package com.mdsproject.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdsproject.backend.agent.DemocracyProposalAgent;
import com.mdsproject.backend.dto.chat.ChatMessageDTO;
import com.mdsproject.backend.dto.democracy.DemocracyDmResponse;
import com.mdsproject.backend.dto.democracy.SubmitConstraintsRequest;
import com.mdsproject.backend.models.*;
import com.mdsproject.backend.models.enums.DemocracySessionStatus;
import com.mdsproject.backend.repositories.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemocracyAgentService {

    private static final String AGENT_EMAIL = "democracy-agent@fairpay.internal";
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");
    private static final List<DemocracySessionStatus> ACTIVE_STATUSES =
            List.of(DemocracySessionStatus.COLLECTING, DemocracySessionStatus.PROPOSING, DemocracySessionStatus.VOTING);

    private static final Pattern TRIGGER_PHRASE = Pattern.compile(
            "(?i)\\b(we need|we want|let'?s (book|buy|get|rent|find)|should we (book|buy|get|rent|find)|how about (booking|buying|getting|renting)|can we get|we should (book|buy|get|rent))\\b");
    private static final Pattern PURCHASE_KEYWORD = Pattern.compile(
            "(?i)\\b(airbnb|hotel|hostel|apartment|accommodation|villa|cabin|resort|booking|rent|trip|vacation|holiday|retreat|office|projector|equipment|purchase|buy)\\b");

    private final DemocracySessionRepository sessionRepository;
    private final MemberConstraintRepository constraintRepository;
    private final DemocracyVoteRepository voteRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GroupMembershipRepository membershipRepository;
    private final FairPayGroupRepository groupRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    public boolean isTrigger(String content) {
        return TRIGGER_PHRASE.matcher(content).find() && PURCHASE_KEYWORD.matcher(content).find();
    }

    @Transactional
    public void handleTrigger(UUID groupId, String goalText) {
        List<DemocracySession> active = sessionRepository.findByGroupIdAndStatusIn(groupId, ACTIVE_STATUSES);

        // A session still gathering input or generating proposals is genuinely in progress,
        // so defer to it. A session left in VOTING (proposals already published) should not
        // block forever — close it so this new request can start a fresh vote.
        boolean setupInProgress = active.stream().anyMatch(s ->
                s.getStatus() == DemocracySessionStatus.COLLECTING
                        || s.getStatus() == DemocracySessionStatus.PROPOSING);
        if (setupInProgress) {
            return;
        }
        for (DemocracySession stale : active) {
            stale.setStatus(DemocracySessionStatus.CLOSED);
            sessionRepository.save(stale);
        }

        List<GroupMembership> memberships = membershipRepository.findByGroupId(groupId);
        if (memberships.isEmpty()) return;

        FairPayGroup group = groupRepository.findById(groupId).orElse(null);
        if (group == null) return;

        DemocracySession session = new DemocracySession();
        session.setGroupId(groupId);
        session.setGoalText(goalText);
        session.setStatus(DemocracySessionStatus.COLLECTING);
        session.setMemberCount(memberships.size());
        sessionRepository.save(session);

        List<String> memberEmails = memberships.stream()
                .map(m -> m.getUser().getEmail())
                .collect(Collectors.toList());

        String memberList = memberEmails.stream()
                .map(e -> "• " + e)
                .collect(Collectors.joining("\n"));

        String triggerContent = String.format(
                "Democracy Agent activated for: \"%s\"\n" +
                        "I will DM each member privately to collect preferences.\n" +
                        "Members notified (%d):\n%s",
                goalText, memberEmails.size(), memberList);

        broadcastAgentMessage(groupId, triggerContent, "DEMOCRACY_TRIGGER", null);

        for (GroupMembership membership : memberships) {
            User member = membership.getUser();
            Notification notification = new Notification();
            notification.setUser(member);
            notification.setGroup(group);
            notification.setMessage("Democracy Agent: Please submit your preferences for \"" + goalText + "\"");
            notification.setTargetUrl("/groups/" + groupId + "?democracyDm=1");
            notification.setRead(false);
            notificationRepository.save(notification);
        }

        log.info("Democracy session started for group {} goal: {}", groupId, goalText);
    }

    @Transactional
    public void submitConstraints(UUID groupId, String memberEmail, SubmitConstraintsRequest req) {
        DemocracySession session = sessionRepository
                .findFirstByGroupIdAndStatusOrderByCreatedAtDesc(groupId, DemocracySessionStatus.COLLECTING)
                .orElseThrow(() -> new IllegalStateException("No active collecting session for this group"));

        if (constraintRepository.existsBySessionIdAndMemberEmail(session.getId(), memberEmail)) {
            throw new IllegalStateException("You have already submitted your preferences");
        }

        MemberConstraint constraint = new MemberConstraint();
        constraint.setSessionId(session.getId());
        constraint.setMemberEmail(memberEmail);
        constraint.setStartDate(req.getStartDate());
        constraint.setEndDate(req.getEndDate());
        constraint.setDealBreakers(req.getDealBreakers());
        constraint.setMaxContribution(req.getMaxContribution());
        constraintRepository.save(constraint);

        long submitted = constraintRepository.countBySessionId(session.getId());
        long total = session.getMemberCount();

        broadcastAgentMessage(groupId,
                String.format("Preference received (%d/%d members responded)", submitted, total),
                "DEMOCRACY_PROGRESS", null);

        if (submitted >= total) {
            generateAndBroadcastProposals(session);
        }
    }

    public DemocracyDmResponse getPendingDm(UUID groupId, String memberEmail) {
        Optional<DemocracySession> sessionOpt = sessionRepository
                .findFirstByGroupIdAndStatusOrderByCreatedAtDesc(groupId, DemocracySessionStatus.COLLECTING);

        if (sessionOpt.isEmpty()) {
            return new DemocracyDmResponse(false, null, null);
        }

        DemocracySession session = sessionOpt.get();
        boolean alreadySubmitted = constraintRepository
                .existsBySessionIdAndMemberEmail(session.getId(), memberEmail);

        if (alreadySubmitted) {
            return new DemocracyDmResponse(false, null, null);
        }

        return new DemocracyDmResponse(true, session.getId(), session.getGoalText());
    }

    @Transactional
    public Map<String, Object> castVote(UUID groupId, String voterEmail, UUID sessionId, int optionIndex) {
        DemocracySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("Session does not belong to this group");
        }
        if (session.getStatus() != DemocracySessionStatus.VOTING) {
            throw new IllegalStateException("Session is not in VOTING state");
        }
        if (voteRepository.existsBySessionIdAndVoterEmail(sessionId, voterEmail)) {
            throw new IllegalStateException("You have already voted");
        }
        if (optionIndex < 0 || optionIndex > 2) {
            throw new IllegalArgumentException("Invalid option index");
        }

        DemocracyVote vote = new DemocracyVote();
        vote.setSessionId(sessionId);
        vote.setVoterEmail(voterEmail);
        vote.setOptionIndex(optionIndex);
        voteRepository.save(vote);

        Map<String, Object> voteState = buildVoteState(session);
        broadcastVoteUpdate(groupId, sessionId, voteState);

        // Once everyone has voted, close the session and announce the winner. This also
        // frees the group so the Democracy Agent can be triggered again for a new request.
        long totalVotes = voteRepository.findBySessionId(sessionId).size();
        if (totalVotes >= session.getMemberCount()) {
            closeSessionWithResult(session, voteState);
        }
        return voteState;
    }

    private void closeSessionWithResult(DemocracySession session, Map<String, Object> voteState) {
        session.setStatus(DemocracySessionStatus.CLOSED);
        sessionRepository.save(session);

        @SuppressWarnings("unchecked")
        Map<String, Long> votes = (Map<String, Long>) voteState.get("votes");
        int winnerIdx = 0;
        long best = -1;
        for (int i = 0; i < 3; i++) {
            long count = votes != null ? votes.getOrDefault(String.valueOf(i), 0L) : 0L;
            if (count > best) {
                best = count;
                winnerIdx = i;
            }
        }

        String title = optionTitle(session, winnerIdx);
        String content = String.format(
                "🏆 Voting closed! Winner: Option %s — %s (%d vote%s). " +
                        "The Democracy Agent is ready for the next request.",
                (char) ('A' + winnerIdx), title, best, best == 1 ? "" : "s");
        broadcastAgentMessage(session.getGroupId(), content, "DEMOCRACY_PROGRESS", null);
    }

    private String optionTitle(DemocracySession session, int idx) {
        try {
            JsonNode options = objectMapper.readTree(session.getProposalsJson()).path("options");
            if (options.isArray() && idx < options.size()) {
                return options.get(idx).path("title").asText("Option " + (idx + 1));
            }
        } catch (Exception ignored) {
            // fall through to default label
        }
        return "Option " + (idx + 1);
    }

    public Map<String, Object> getSessionState(UUID groupId, String requesterEmail) {
        Optional<DemocracySession> sessionOpt = sessionRepository
                .findFirstByGroupIdAndStatusInOrderByCreatedAtDesc(groupId, ACTIVE_STATUSES);

        if (sessionOpt.isEmpty()) {
            return Map.of("active", false);
        }

        DemocracySession session = sessionOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("active", true);
        result.put("sessionId", session.getId());
        result.put("goalText", session.getGoalText());
        result.put("status", session.getStatus().name());

        if (session.getStatus() == DemocracySessionStatus.VOTING) {
            result.putAll(buildVoteState(session));
            result.put("hasVoted", voteRepository.existsBySessionIdAndVoterEmail(session.getId(), requesterEmail));
        }
        return result;
    }

    private void generateAndBroadcastProposals(DemocracySession session) {
        session.setStatus(DemocracySessionStatus.PROPOSING);
        sessionRepository.save(session);

        broadcastAgentMessage(session.getGroupId(),
                "All preferences collected! Generating the 3 best options...",
                "DEMOCRACY_PROGRESS", null);

        try {
            String proposalsJson = generateProposalsJson(session);
            session.setProposalsJson(proposalsJson);
            session.setStatus(DemocracySessionStatus.VOTING);
            sessionRepository.save(session);

            String metadata = buildProposalMetadata(session, proposalsJson);
            broadcastAgentMessage(session.getGroupId(),
                    "Here are your 3 options! Vote for your favourite:",
                    "DEMOCRACY_PROPOSAL", metadata);

            log.info("Democracy proposals generated for session {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to generate democracy proposals for session {}: {}", session.getId(), e.getMessage());
            session.setStatus(DemocracySessionStatus.COLLECTING);
            sessionRepository.save(session);
            broadcastAgentMessage(session.getGroupId(),
                    "Proposal generation failed. Please try again by re-submitting preferences.",
                    "DEMOCRACY_PROGRESS", null);
        }
    }

    private String generateProposalsJson(DemocracySession session) {
        List<MemberConstraint> constraints = constraintRepository.findBySessionId(session.getId());
        String summary = buildConstraintsSummary(constraints);

        if (!isAiConfigured()) {
            return fallbackProposalsJson(session.getGoalText(), constraints);
        }

        DemocracyProposalAgent agent = AiServices.builder(DemocracyProposalAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();

        String raw = agent.generateProposalsJson(session.getGoalText(), session.getMemberCount(), summary);
        return extractJson(raw);
    }

    private String buildConstraintsSummary(List<MemberConstraint> constraints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < constraints.size(); i++) {
            MemberConstraint c = constraints.get(i);
            sb.append("Member ").append(i + 1).append(":\n");
            if (c.getStartDate() != null && !c.getStartDate().isBlank())
                sb.append("  Dates: ").append(c.getStartDate()).append(" to ").append(c.getEndDate()).append("\n");
            if (c.getDealBreakers() != null && !c.getDealBreakers().isBlank())
                sb.append("  Deal-breakers: ").append(c.getDealBreakers()).append("\n");
            if (c.getMaxContribution() != null)
                sb.append("  Max contribution: €").append(String.format("%.0f", c.getMaxContribution())).append("\n");
        }
        return sb.toString();
    }

    private String buildProposalMetadata(DemocracySession session, String proposalsJson) {
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("sessionId", session.getId().toString());
            meta.put("goal", session.getGoalText());
            JsonNode options = objectMapper.readTree(proposalsJson).path("options");
            meta.put("options", options);
            meta.put("votes", Map.of("0", 0, "1", 0, "2", 0));
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            return "{\"error\":\"parse_failed\"}";
        }
    }

    private Map<String, Object> buildVoteState(DemocracySession session) {
        Map<String, Object> state = new HashMap<>();
        state.put("sessionId", session.getId());

        Map<String, Long> voteCounts = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            voteCounts.put(String.valueOf(i), voteRepository.countBySessionIdAndOptionIndex(session.getId(), i));
        }
        state.put("votes", voteCounts);

        List<String> voters = voteRepository.findBySessionId(session.getId())
                .stream().map(DemocracyVote::getVoterEmail).collect(Collectors.toList());
        state.put("votedBy", voters);
        return state;
    }

    private void broadcastAgentMessage(UUID groupId, String content, String messageType, String metadata) {
        ChatMessage msg = new ChatMessage();
        msg.setGroupId(groupId);
        msg.setSenderEmail(AGENT_EMAIL);
        msg.setContent(content);
        msg.setMessageType(messageType);
        msg.setMetadata(metadata);
        ChatMessage saved = chatMessageRepository.save(msg);

        ChatMessageDTO dto = new ChatMessageDTO(
                saved.getId(), saved.getGroupId(), saved.getSenderEmail(),
                saved.getContent(), saved.getCreatedAt(),
                saved.getMessageType(), saved.getMetadata());

        messagingTemplate.convertAndSend("/topic/chat/" + groupId, dto);
    }

    private void broadcastVoteUpdate(UUID groupId, UUID sessionId, Map<String, Object> voteState) {
        try {
            Map<String, Object> payload = new HashMap<>(voteState);
            payload.put("messageType", "DEMOCRACY_VOTE_UPDATE");

            ChatMessageDTO dto = new ChatMessageDTO(
                    null, groupId, AGENT_EMAIL,
                    objectMapper.writeValueAsString(payload),
                    LocalDateTime.now(),
                    "DEMOCRACY_VOTE_UPDATE",
                    objectMapper.writeValueAsString(payload));

            messagingTemplate.convertAndSend("/topic/chat/" + groupId, dto);
        } catch (Exception e) {
            log.warn("Failed to broadcast vote update: {}", e.getMessage());
        }
    }

    private boolean isAiConfigured() {
        return aiEnabled && aiApiKey != null && !aiApiKey.isBlank()
                && !aiApiKey.startsWith("your_") && !aiApiKey.startsWith("PASTE_YOUR");
    }

    private String extractJson(String raw) {
        if (raw == null) throw new IllegalArgumentException("Empty AI response");
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        Matcher m = JSON_BLOCK.matcher(trimmed);
        if (m.find()) return m.group();
        return trimmed;
    }

    private String fallbackProposalsJson(String goal, List<MemberConstraint> constraints) {
        OptionalDouble lowestBudget = constraints.stream()
                .filter(c -> c.getMaxContribution() != null)
                .mapToDouble(MemberConstraint::getMaxContribution)
                .min();
        double budget = lowestBudget.orElse(100.0);

        return String.format("""
                {
                  "options": [
                    {
                      "index": 0,
                      "title": "Option A: Budget-Friendly Pick",
                      "description": "A practical, affordable option for '%s' that fits within everyone's budget. Prioritises essential features over luxury.",
                      "estimatedPricePerPerson": %.0f,
                      "highlights": ["Fits all budgets", "Central location", "Good reviews"],
                      "fairnessScore": 90
                    },
                    {
                      "index": 1,
                      "title": "Option B: Balanced Choice",
                      "description": "A mid-range option for '%s' balancing comfort and cost. Suitable for most preferences.",
                      "estimatedPricePerPerson": %.0f,
                      "highlights": ["Good value", "Popular choice", "Flexible dates"],
                      "fairnessScore": 80
                    },
                    {
                      "index": 2,
                      "title": "Option C: Premium Experience",
                      "description": "A premium option for '%s' with extra amenities. Slightly above budget but offers the best experience.",
                      "estimatedPricePerPerson": %.0f,
                      "highlights": ["Top-rated", "Extra amenities", "Best reviews"],
                      "fairnessScore": 70
                    }
                  ]
                }
                """,
                goal, budget * 0.7,
                goal, budget * 0.85,
                goal, budget);
    }
}
