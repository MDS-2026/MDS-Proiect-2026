package com.mdsproject.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdsproject.backend.agent.LiquidityAdvisorAgent;
import com.mdsproject.backend.dto.chat.ChatMessageDTO;
import com.mdsproject.backend.dto.liquidity.LedgerSwapProposalDto;
import com.mdsproject.backend.dto.liquidity.LiquidityForecastResponse;
import com.mdsproject.backend.exceptions.BadRequestException;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Asset;
import com.mdsproject.backend.models.ChatMessage;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.enums.AssetType;
import com.mdsproject.backend.models.enums.AuditAction;
import com.mdsproject.backend.models.enums.TransactionStatus;
import com.mdsproject.backend.repositories.AssetRepository;
import com.mdsproject.backend.repositories.ChatMessageRepository;
import com.mdsproject.backend.repositories.FairPayGroupRepository;
import com.mdsproject.backend.repositories.TransactionRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Predictive Liquidity Agent (Treasury Agent).
 *
 * <p>Watches the gap between liquid cash and committed-but-unsettled transactions. When a
 * shortfall is predicted it warns the group in chat and proposes internal ledger swaps —
 * converting non-cash assets (gift cards, vouchers, miles) into cash on the group ledger so
 * outgoing transactions don't fail.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveLiquidityService {

    private static final String AGENT_EMAIL = "treasury-agent@fairpay.internal";
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    /** Transactions in these states draw down cash but have not settled yet. */
    private static final Set<TransactionStatus> UNSETTLED = EnumSet.of(
            TransactionStatus.PENDING,
            TransactionStatus.PENDING_GROUP_APPROVAL,
            TransactionStatus.PENDING_MANUAL_APPROVAL);

    private final AssetRepository assetRepository;
    private final TransactionRepository transactionRepository;
    private final FairPayGroupRepository groupRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogService auditLogService;
    private final AlertService alertService;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Builds the cash-flow forecast for a group without mutating anything.
     */
    public LiquidityForecastResponse forecast(UUID groupId) {
        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        List<Asset> assets = assetRepository.findByGroupId(groupId);

        double availableCash = assets.stream()
                .filter(a -> a.getType() == AssetType.CASH)
                .mapToDouble(this::eur)
                .sum();

        double upcomingObligations = transactionRepository.findByWalletGroupId(groupId).stream()
                .filter(tx -> UNSETTLED.contains(tx.getStatus()))
                .mapToDouble(tx -> tx.getAmount() != null ? tx.getAmount() : 0.0)
                .sum();

        double projectedBalance = availableCash - upcomingObligations;
        double shortfall = Math.max(0.0, upcomingObligations - availableCash);

        List<LedgerSwapProposalDto> candidates = swapCandidates(assets);
        List<LedgerSwapProposalDto> recommended = selectSwapsForShortfall(candidates, shortfall);

        LiquidityForecastResponse response = new LiquidityForecastResponse();
        response.setGroupId(groupId);
        response.setGroupName(group.getName());
        response.setAvailableCash(round(availableCash));
        response.setUpcomingObligations(round(upcomingObligations));
        response.setProjectedBalance(round(projectedBalance));
        response.setShortfall(round(shortfall));
        response.setSwapCandidates(candidates);
        response.setRecommendedSwaps(recommended);

        applyAssessment(response, group, candidates, recommended);
        return response;
    }

    /**
     * Computes the forecast and, when there is a shortfall, broadcasts a warning to the group
     * chat and notifies members. Safe to call from transaction flows: never throws.
     *
     * @return the forecast, or {@code null} if it could not be produced.
     */
    public LiquidityForecastResponse checkAndWarn(UUID groupId) {
        try {
            LiquidityForecastResponse forecast = forecast(groupId);
            if (!"OK".equals(forecast.getSeverity())) {
                broadcastWarning(forecast);
            }
            return forecast;
        } catch (Exception e) {
            log.warn("Liquidity check failed for group {}: {}", groupId, e.getMessage());
            return null;
        }
    }

    /**
     * Executes an internal ledger swap: converts a non-cash asset into liquid cash, keeping the
     * group's total pooled value unchanged. Returns the refreshed forecast.
     */
    @Transactional
    public LiquidityForecastResponse executeSwap(UUID groupId, UUID assetId, String email) {
        FairPayGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        if (asset.getGroup() == null || !asset.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Asset does not belong to this group");
        }
        if (asset.getType() == AssetType.CASH) {
            throw new BadRequestException("Asset is already cash; nothing to swap");
        }

        double cashGained = eur(asset);
        String fromType = asset.getType().name();
        String fromProvider = asset.getProvider();

        // Convert the asset to liquid cash on the ledger. Total pooled value is preserved
        // (estimatedEurValue is unchanged), so only liquidity improves.
        asset.setType(AssetType.CASH);
        asset.setAmount(cashGained);
        asset.setAmountUnit("EUR");
        asset.setProvider("Internal swap (" + fromProvider + ")");
        asset.setExpiryDate(null);
        assetRepository.save(asset);

        auditLogService.log(AuditAction.LEDGER_SWAP_EXECUTED, email, groupId, asset.getId(),
                String.format("Internal ledger swap: %s from %s converted to €%.2f cash to cover liquidity needs",
                        fromType, fromProvider, cashGained));

        LiquidityForecastResponse forecast = forecast(groupId);

        broadcastAgentMessage(groupId,
                String.format("Internal swap done: %s from %s → €%.2f cash. Available cash is now €%.2f.",
                        fromType, fromProvider, cashGained, forecast.getAvailableCash()),
                "LIQUIDITY_SWAP_DONE", forecast);

        alertService.sendGroupAlert(group,
                String.format("Treasury Agent: swapped %s (%s) for €%.2f cash to keep transactions funded.",
                        fromType, fromProvider, cashGained),
                "/groups/" + groupId);

        return forecast;
    }

    // ---------------------------------------------------------------------
    // Assessment (AI + heuristic)
    // ---------------------------------------------------------------------

    private void applyAssessment(LiquidityForecastResponse response, FairPayGroup group,
                                 List<LedgerSwapProposalDto> candidates,
                                 List<LedgerSwapProposalDto> recommended) {
        if (isAiConfigured() && !candidates.isEmpty()) {
            try {
                aiAssessment(response, group, candidates);
                return;
            } catch (Exception e) {
                log.warn("Liquidity AI assessment failed, using heuristic: {}", e.getMessage());
            }
        }
        heuristicAssessment(response, recommended);
    }

    private void aiAssessment(LiquidityForecastResponse response, FairPayGroup group,
                              List<LedgerSwapProposalDto> candidates) throws Exception {
        LiquidityAdvisorAgent agent = AiServices.builder(LiquidityAdvisorAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .build();

        String raw = agent.assessLiquidityJson(
                group.getName() != null ? group.getName() : "Group",
                response.getAvailableCash(),
                response.getUpcomingObligations(),
                response.getProjectedBalance(),
                response.getShortfall(),
                renderCandidates(candidates));

        JsonNode root = objectMapper.readTree(extractJson(raw));

        String severity = normalizeSeverity(root.path("severity").asText("OK"), response.getShortfall());
        response.setSeverity(severity);
        response.setMessage(root.path("message").asText(defaultMessage(response)));
        response.setRationale(root.path("rationale").asText(""));

        JsonNode idx = root.path("recommendedSwapIndexes");
        if (idx.isArray() && idx.size() > 0) {
            List<LedgerSwapProposalDto> picked = new ArrayList<>();
            for (JsonNode n : idx) {
                int i = n.asInt(-1);
                if (i >= 0 && i < candidates.size() && !picked.contains(candidates.get(i))) {
                    picked.add(candidates.get(i));
                }
            }
            if (!picked.isEmpty()) {
                response.setRecommendedSwaps(picked);
            }
        }
    }

    private void heuristicAssessment(LiquidityForecastResponse response,
                                     List<LedgerSwapProposalDto> recommended) {
        response.setSeverity(severityFor(response));
        response.setMessage(defaultMessage(response));
        if (recommended.isEmpty()) {
            response.setRationale("");
        } else {
            double covered = recommended.stream()
                    .mapToDouble(s -> s.getCashValueEur() != null ? s.getCashValueEur() : 0.0)
                    .sum();
            response.setRationale(String.format(
                    "Swapping %d non-cash asset(s) frees €%.2f, covering the shortfall (soonest-to-expire first).",
                    recommended.size(), covered));
        }
    }

    private String severityFor(LiquidityForecastResponse r) {
        if (r.getShortfall() != null && r.getShortfall() > 0.0) {
            return "CRITICAL";
        }
        double obligations = r.getUpcomingObligations() != null ? r.getUpcomingObligations() : 0.0;
        double projected = r.getProjectedBalance() != null ? r.getProjectedBalance() : 0.0;
        if (obligations > 0 && projected < obligations * 0.20) {
            return "WATCH";
        }
        return "OK";
    }

    private String normalizeSeverity(String aiSeverity, Double shortfall) {
        // A real shortfall always overrides the model into CRITICAL.
        if (shortfall != null && shortfall > 0.0) {
            return "CRITICAL";
        }
        String s = aiSeverity == null ? "" : aiSeverity.trim().toUpperCase();
        return switch (s) {
            case "CRITICAL", "WATCH", "OK" -> s;
            default -> "OK";
        };
    }

    private String defaultMessage(LiquidityForecastResponse r) {
        if ("CRITICAL".equals(r.getSeverity()) || (r.getShortfall() != null && r.getShortfall() > 0.0)) {
            return String.format(
                    "Predicted cash shortfall of €%.2f: €%.2f in cash vs €%.2f committed. "
                            + "Swap a non-cash asset for cash to avoid failed transactions.",
                    r.getShortfall(), r.getAvailableCash(), r.getUpcomingObligations());
        }
        if ("WATCH".equals(r.getSeverity())) {
            return String.format(
                    "Cash is running tight: €%.2f left after €%.2f of upcoming transactions.",
                    r.getProjectedBalance(), r.getUpcomingObligations());
        }
        return String.format("Liquidity healthy: €%.2f cash covers €%.2f of upcoming transactions.",
                r.getAvailableCash(), r.getUpcomingObligations());
    }

    // ---------------------------------------------------------------------
    // Swap selection
    // ---------------------------------------------------------------------

    private List<LedgerSwapProposalDto> swapCandidates(List<Asset> assets) {
        return assets.stream()
                .filter(a -> a.getType() != AssetType.CASH)
                .filter(a -> eur(a) > 0.0)
                .map(this::toProposal)
                .sorted(candidateOrder())
                .collect(Collectors.toList());
    }

    /** Soonest-to-expire first, then smallest value (preserve flexibility). */
    private Comparator<LedgerSwapProposalDto> candidateOrder() {
        return Comparator
                .comparing((LedgerSwapProposalDto p) -> p.getDaysToExpiry() == null ? Integer.MAX_VALUE : p.getDaysToExpiry())
                .thenComparing(p -> p.getCashValueEur() == null ? Double.MAX_VALUE : p.getCashValueEur());
    }

    private List<LedgerSwapProposalDto> selectSwapsForShortfall(
            List<LedgerSwapProposalDto> candidates, double shortfall) {
        if (shortfall <= 0.0) {
            return new ArrayList<>();
        }
        List<LedgerSwapProposalDto> picked = new ArrayList<>();
        double covered = 0.0;
        for (LedgerSwapProposalDto c : candidates) {
            if (covered >= shortfall) {
                break;
            }
            picked.add(c);
            covered += c.getCashValueEur() != null ? c.getCashValueEur() : 0.0;
        }
        return picked;
    }

    private LedgerSwapProposalDto toProposal(Asset asset) {
        Integer daysToExpiry = null;
        if (asset.getExpiryDate() != null) {
            daysToExpiry = (int) ChronoUnit.DAYS.between(LocalDate.now(), asset.getExpiryDate());
        }
        String reason;
        if (daysToExpiry != null && daysToExpiry <= 30) {
            reason = "expires in " + daysToExpiry + " day(s)";
        } else if (daysToExpiry != null) {
            reason = "non-cash, expires in " + daysToExpiry + " day(s)";
        } else {
            reason = "non-cash asset available to convert";
        }
        return new LedgerSwapProposalDto(
                asset.getId(),
                asset.getType().name(),
                asset.getProvider(),
                round(eur(asset)),
                asset.getExpiryDate(),
                daysToExpiry,
                reason);
    }

    private String renderCandidates(List<LedgerSwapProposalDto> candidates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            LedgerSwapProposalDto c = candidates.get(i);
            sb.append('[').append(i).append("] ")
                    .append(c.getAssetType()).append(" from ").append(c.getProvider())
                    .append(" — €").append(String.format("%.2f", c.getCashValueEur()))
                    .append(c.getDaysToExpiry() != null
                            ? ", expires in " + c.getDaysToExpiry() + " day(s)"
                            : ", no expiry")
                    .append('\n');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Broadcasting
    // ---------------------------------------------------------------------

    private void broadcastWarning(LiquidityForecastResponse forecast) {
        broadcastAgentMessage(forecast.getGroupId(), forecast.getMessage(), "LIQUIDITY_WARNING", forecast);

        FairPayGroup group = groupRepository.findById(forecast.getGroupId()).orElse(null);
        if (group != null) {
            alertService.sendGroupAlert(group, "Treasury Agent: " + forecast.getMessage(),
                    "/groups/" + forecast.getGroupId());
        }

        auditLogService.log(AuditAction.LIQUIDITY_WARNING, AGENT_EMAIL, forecast.getGroupId(), null,
                String.format("%s — cash €%.2f vs obligations €%.2f (shortfall €%.2f); %d swap(s) proposed",
                        forecast.getSeverity(), forecast.getAvailableCash(), forecast.getUpcomingObligations(),
                        forecast.getShortfall(), forecast.getRecommendedSwaps().size()));
    }

    private void broadcastAgentMessage(UUID groupId, String content, String messageType,
                                       LiquidityForecastResponse forecast) {
        String metadata = null;
        try {
            metadata = objectMapper.writeValueAsString(forecast);
        } catch (Exception e) {
            log.warn("Failed to serialise liquidity metadata: {}", e.getMessage());
        }

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

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private double eur(Asset asset) {
        return asset.getEstimatedEurValue() != null ? asset.getEstimatedEurValue() : 0.0;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
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
}
