package com.mdsproject.backend.services;

import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiValidationService {

    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate;

    // Make sure your application.properties has: 
    // ai.endpoint=https://api.groq.com/openai/v1/chat/completions
    @Value("${ai.endpoint:}")
    private String aiEndpoint;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.timeout-ms:5000}")
    private long aiTimeoutMs;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    /**
     * Validates a transaction against the wallet's purpose using AI logic.
     */
    public boolean validateTransaction(Wallet wallet, String merchant, String category, Double amount, String email) {
        String purpose = (wallet.getPurpose() == null ? "" : wallet.getPurpose()).toLowerCase(Locale.ROOT);
        String name = wallet.getName().toLowerCase(Locale.ROOT);
        String m = safeLower(merchant);
        String c = safeLower(category);

        log.info("AI validating transaction: Merchant={}, Category={} for Wallet='{}' (Purpose='{}')", 
                merchant, category, wallet.getName(), wallet.getPurpose());

        boolean isValid;
        String reason;

        // Try external AI service if configured
        if (isAiConfigured()) {
            try {
                isValid = validateViaExternalAi(wallet, merchant, category, amount);
                reason = isValid ? "AI approved transaction" : "AI detected merchant mismatch";
                log.info("External AI decision: {}", reason);
            } catch (Exception e) {
                log.error("External AI service unavailable: {}", e.getMessage());
                auditLogService.log(AuditAction.AI_DECISION, "AI_SYSTEM", 
                        wallet.getGroup().getId(), wallet.getId(), 
                        "AI Service unavailable — PENDING_MANUAL_APPROVAL");
                throw new TransactionValidationException("AI service unavailable", e);
            }
        } else {
            // Heuristic fallback logic
            isValid = true;
            reason = "Transaction matches wallet purpose";

            if ((name.contains("flight") || purpose.contains("flight") || purpose.contains("travel")) 
                    && (c.contains("grocery") || m.contains("supermarket") || m.contains("lidl") || m.contains("walmart") || m.contains("coop"))) {
                isValid = false;
                reason = "Mismatch: Grocery/Supermarket on Travel/Flight wallet";
            } else if ((name.contains("food") || purpose.contains("food") || purpose.contains("restaurant")) && c.contains("electronics")) {
                isValid = false;
                reason = "Mismatch: Electronics on Food/Restaurant wallet";
            } else if ((name.contains("office") || purpose.contains("office")) && (c.contains("entertainment") || m.contains("cinema") || m.contains("spotify"))) {
                isValid = false;
                reason = "Mismatch: Entertainment on Office wallet";
            }
        }

        auditLogService.log(AuditAction.AI_DECISION, "AI_SYSTEM", 
                wallet.getGroup().getId(), wallet.getId(), 
                "AI Validation for transaction at " + merchant + ": " + (isValid ? "APPROVED" : "DECLINED") + " - " + reason);

        return isValid;
    }

    private boolean isAiConfigured() {
        return aiEnabled && aiEndpoint != null && !aiEndpoint.isEmpty() && aiApiKey != null && !aiApiKey.isEmpty();
    }

    /**
     * Call external AI service (Groq/OpenAI compatible) to validate transaction.
     */
    @SuppressWarnings("unchecked")
    private boolean validateViaExternalAi(Wallet wallet, String merchant, String category, Double amount) {
        try {
            // Updated strict prompt
            String prompt = String.format(
                "Analyze this transaction for a shared wallet. " +
                "Wallet Name: '%s', Purpose: '%s'. " +
                "Transaction: Merchant='%s', Category='%s', Amount='%.2f'. " +
                "Does this transaction logically match the wallet's purpose? " +
                "Answer ONLY with the word 'true' or 'false'. Do not provide any other text or explanation.",
                wallet.getName(), wallet.getPurpose(), merchant, category, amount
            );

            // Structure the request for Groq / OpenAI API
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> request = new HashMap<>();
            request.put("model", "llama-3.3-70b-versatile"); // Use Groq model
            request.put("messages", List.of(message));
            request.put("temperature", 0.0); // Force deterministic output

            // Add authorization headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("AI Request sent to: {}", aiEndpoint);

            Map<String, Object> response = restTemplate.postForObject(
                    aiEndpoint,
                    entity,
                    Map.class
            );

            // Parse Groq / OpenAI standard response
            var choices = (java.util.List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var messageObj = (Map<String, Object>) choices.get(0).get("message");
                String text = (String) messageObj.get("content");
                
                log.info("AI Raw Response: {}", text);
                
                // Clean text and check boolean value
                if (text != null) {
                    return text.trim().toLowerCase().contains("true");
                }
            }

            return true; // Fallback to approve if AI response is weird

        } catch (RestClientException e) {
            log.error("Failed to call AI service at {}: {}", aiEndpoint, e.getMessage());
            throw new TransactionValidationException("AI service call failed", e);
        }
    }

    public String getValidationReason(Wallet wallet, String merchant, String category) {
        String purpose = (wallet.getPurpose() == null ? "" : wallet.getPurpose()).toLowerCase(Locale.ROOT);
        String name = wallet.getName().toLowerCase(Locale.ROOT);
        String m = safeLower(merchant);
        String c = safeLower(category);

        if ((name.contains("flight") || purpose.contains("flight")) && (c.contains("grocery") || m.contains("supermarket"))) {
            return "Grocery/Supermarket transaction not allowed on Travel/Flight wallet";
        } else if ((name.contains("food") || purpose.contains("food")) && c.contains("electronics")) {
            return "Electronics transaction not allowed on Food wallet";
        }
        return "Transaction does not match wallet purpose";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public static class TransactionValidationException extends RuntimeException {
        public TransactionValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}