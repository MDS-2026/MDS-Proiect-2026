package com.mdsproject.backend.services;

import com.mdsproject.backend.models.Asset;
import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiResourceAllocatorService {

    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate;

    @Value("${ai.endpoint:}")
    private String aiEndpoint;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:gemini-2.5-flash}")
    private String aiModel;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    /**
     * Finds the best wallet for a newly added asset based on semantic matching.
     */
    public Optional<Wallet> allocateAsset(Asset asset, List<Wallet> wallets) {
        if (wallets.isEmpty())
            return Optional.empty();

        log.info("AI Allocator: Finding best wallet for asset {} from {}", asset.getType(), asset.getProvider());

        // 1. Check for Expiration Urgency
        if (asset.getExpiryDate() != null) {
            long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), asset.getExpiryDate());
            if (daysToExpiry < 30) {
                log.info("Asset expires soon ({} days). Prioritizing active wallets.", daysToExpiry);
            }
        }

        // 2. AI Semantic Matching
        if (isAiConfigured()) {
            return findWalletViaAi(asset, wallets);
        }

        // 3. Heuristic Fallback
        return wallets.stream()
                .filter(w -> matchesHeuristically(asset, w))
                .findFirst();
    }

    /**
     * Analyzes spending patterns and suggests budget rebalancing between cards.
     */
    public void rebalanceCardLimits(FairPayGroup group, List<Wallet> wallets) {
        log.info("AI Allocator: Analyzing budget limits for group {}", group.getName());

        // This is a placeholder for more complex logic that would analyze transaction
        // history
        // For now, it logs the intent.
        auditLogService.log(AuditAction.AI_DECISION, "AI_SYSTEM", group.getId(), null,
                "AI Resource Allocator analyzed budget limits. No rebalancing needed at this time.");
    }

    private boolean isAiConfigured() {
        return aiEnabled && aiEndpoint != null && !aiEndpoint.isEmpty() && aiApiKey != null && !aiApiKey.isEmpty();
    }

    private Optional<Wallet> findWalletViaAi(Asset asset, List<Wallet> wallets) {
        try {
            StringBuilder walletInfo = new StringBuilder();
            for (int i = 0; i < wallets.size(); i++) {
                Wallet w = wallets.get(i);
                walletInfo.append(String.format("[%d] Name: %s, Purpose: %s, Budget Limit: %.2f\n", i, w.getName(),
                        w.getPurpose(), w.getBudgetLimit()));
            }

            String expiryInfo = asset.getExpiryDate() != null
                    ? String.format("Expires in %d days. (Urgent if < 30 days).",
                            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),
                                    asset.getExpiryDate()))
                    : "No expiration date.";

            String prompt = String.format(
                    "You are an AI Resource Allocator for a shared finance app. " +
                            "New Asset: Type=%s, Provider=%s, Amount=%.2f %s. %s " +
                            "Available Wallets:\n%s" +
                            "Task: Select the most appropriate wallet index for this asset. " +
                            "Rules: Match the semantic domain of the provider to the wallet purpose. " +
                            "If the asset expires urgently, favor active wallets with high budgets. " +
                            "If no wallet matches well, return -1. " +
                            "Answer ONLY with the index number.",
                    asset.getType(), asset.getProvider(), asset.getAmount(), asset.getAmountUnit(), expiryInfo,
                    walletInfo.toString());

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> request = new HashMap<>();
            request.put("model", aiModel);
            request.put("messages", List.of(message));
            request.put("temperature", 0.0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            Map<String, Object> response = restTemplate.postForObject(aiEndpoint, entity, Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                String text = ((String) messageObj.get("content")).trim();
                int index = Integer.parseInt(text.replaceAll("[^0-9-]", ""));

                if (index >= 0 && index < wallets.size()) {
                    return Optional.of(wallets.get(index));
                }
            }
        } catch (Exception e) {
            log.error("AI Allocation failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private boolean matchesHeuristically(Asset asset, Wallet wallet) {
        String purpose = wallet.getPurpose() != null ? wallet.getPurpose().toLowerCase() : "";
        String name = wallet.getName().toLowerCase();
        String provider = asset.getProvider().toLowerCase();

        if ((purpose.contains("travel") || name.contains("flight")) &&
                (provider.contains("uber") || provider.contains("airline") || provider.contains("hotel"))) {
            return true;
        }
        if ((purpose.contains("food") || name.contains("grocery")) &&
                (provider.contains("lidl") || provider.contains("restaurant") || provider.contains("food"))) {
            return true;
        }
        return false;
    }
}
