package com.mdsproject.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdsproject.backend.agent.ShoppingSuggestionAgent;
import com.mdsproject.backend.dto.wallet.ShoppingSuggestionItemDto;
import com.mdsproject.backend.dto.wallet.ShoppingSuggestionsResponse;
import com.mdsproject.backend.exceptions.BadRequestException;
import com.mdsproject.backend.exceptions.ResourceNotFoundException;
import com.mdsproject.backend.models.Transaction;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.TransactionStatus;
import com.mdsproject.backend.repositories.TransactionRepository;
import com.mdsproject.backend.repositories.WalletRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletShoppingSuggestionService {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    public ShoppingSuggestionsResponse generateSuggestions(UUID groupId, UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (!wallet.getGroup().getId().equals(groupId)) {
            throw new BadRequestException("Wallet does not belong to this group");
        }

        double spent = computeSpent(walletId);
        double remaining = Math.max(0, wallet.getBudgetLimit() - spent);
        String purpose = wallet.getPurpose() != null ? wallet.getPurpose() : "General purchases";

        if (!isAiConfigured()) {
            log.info("AI not configured for shopping suggestions; using heuristic fallback");
            return fallbackSuggestions(wallet, spent, remaining, purpose);
        }

        try {
            String groupName = wallet.getGroup().getName() != null ? wallet.getGroup().getName() : "Group";
            ShoppingSuggestionAgent agent = AiServices.builder(ShoppingSuggestionAgent.class)
                    .chatLanguageModel(chatLanguageModel)
                    .build();

            String rawJson = agent.generateSuggestionsJson(
                    wallet.getName(),
                    purpose,
                    wallet.getBudgetLimit(),
                    spent,
                    remaining,
                    groupName
            );
            return parseAgentResponse(wallet, spent, remaining, rawJson);
        } catch (Exception e) {
            log.warn("Shopping suggestions AI failed, using heuristic fallback: {}", e.getMessage());
            return fallbackSuggestions(wallet, spent, remaining, purpose);
        }
    }

    private boolean isAiConfigured() {
        return aiEnabled
                && aiApiKey != null
                && !aiApiKey.isBlank()
                && !aiApiKey.startsWith("your_")
                && !aiApiKey.startsWith("PASTE_YOUR");
    }

    private double computeSpent(UUID walletId) {
        return transactionRepository.findByWalletId(walletId).stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.APPROVED)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private ShoppingSuggestionsResponse parseAgentResponse(
            Wallet wallet, double spent, double remaining, String raw) throws Exception {

        String json = extractJson(raw);
        JsonNode root = objectMapper.readTree(json);

        String summary = root.path("summary").asText(
                "Suggestions based on wallet purpose and remaining budget.");

        List<ShoppingSuggestionItemDto> items = new ArrayList<>();
        JsonNode suggestions = root.path("suggestions");
        if (suggestions.isArray()) {
            for (JsonNode node : suggestions) {
                ShoppingSuggestionItemDto item = new ShoppingSuggestionItemDto(
                        node.path("productName").asText(""),
                        node.path("description").asText(""),
                        node.path("estimatedPriceEur").asDouble(0),
                        node.path("sourceName").asText(""),
                        node.path("sourceUrl").asText("")
                );
                if (!item.getProductName().isBlank()) {
                    items.add(item);
                }
            }
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("No suggestions in agent response");
        }

        ShoppingSuggestionsResponse response = new ShoppingSuggestionsResponse();
        response.setWalletId(wallet.getId());
        response.setWalletName(wallet.getName());
        response.setWalletPurpose(wallet.getPurpose());
        response.setBudgetLimit(wallet.getBudgetLimit());
        response.setSpentAmount(spent);
        response.setRemainingBudget(remaining);
        response.setSummary(summary);
        response.setSuggestions(items);
        return response;
    }

    private String extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Empty agent response");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        Matcher m = JSON_BLOCK.matcher(trimmed);
        if (m.find()) {
            return m.group();
        }
        return trimmed;
    }

    private ShoppingSuggestionsResponse fallbackSuggestions(
            Wallet wallet, double spent, double remaining, String purpose) {

        String p = purpose.toLowerCase(Locale.ROOT);
        String walletName = wallet.getName() != null ? wallet.getName().toLowerCase(Locale.ROOT) : "";
        List<ShoppingSuggestionItemDto> items = new ArrayList<>();

        if (p.contains("travel") || p.contains("flight") || walletName.contains("travel")) {
            items.add(item("Travel adapter EU", "Universal plug for trips", 15.0, "Amazon",
                    "https://www.amazon.com/s?k=travel+adapter"));
            items.add(item("Cabin luggage", "Light carry-on within budget", Math.min(remaining, 89.0), "eMAG",
                    "https://www.emag.ro/search/cabin+luggage"));
        } else if (p.contains("food") || p.contains("restaurant")) {
            items.add(item("Meal prep containers", "Reusable boxes for office lunches", 25.0, "Altex",
                    "https://www.altex.ro/cauta/containere+bucatarie"));
            items.add(item("Coffee beans 1kg", "Office pantry restock", Math.min(remaining, 45.0), "eMAG",
                    "https://www.emag.ro/search/cafea+bob"));
        } else {
            items.add(item("Office supplies bundle", "Pens, notebooks matching general wallet", 20.0, "Amazon",
                    "https://www.amazon.com/s?k=office+supplies"));
            items.add(item("USB-C hub", "Useful group equipment", Math.min(remaining, 55.0), "Altex",
                    "https://www.altex.ro/cauta/hub+usb-c"));
        }

        ShoppingSuggestionsResponse response = new ShoppingSuggestionsResponse();
        response.setWalletId(wallet.getId());
        response.setWalletName(wallet.getName());
        response.setWalletPurpose(wallet.getPurpose());
        response.setBudgetLimit(wallet.getBudgetLimit());
        response.setSpentAmount(spent);
        response.setRemainingBudget(remaining);
        response.setSummary("Heuristic suggestions (AI unavailable). Remaining budget: €" + String.format("%.2f", remaining));
        response.setSuggestions(items);
        return response;
    }

    private ShoppingSuggestionItemDto item(String name, String desc, double price, String source, String url) {
        return new ShoppingSuggestionItemDto(name, desc, price, source, url);
    }
}
