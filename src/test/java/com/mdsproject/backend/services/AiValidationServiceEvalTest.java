package com.mdsproject.backend.services;

import com.mdsproject.backend.models.FairPayGroup;
import com.mdsproject.backend.models.Wallet;
import com.mdsproject.backend.models.enums.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class AiValidationServiceEvalTest {

    static class FakeAuditLogService extends AuditLogService {
        final List<String> entries = new ArrayList<>();

        FakeAuditLogService() {
            super(null);
        }

        @Override
        public void log(AuditAction action, String email, UUID groupId, UUID targetEntityId, String details) {
            entries.add(action + ":" + details);
        }
    }

    
    static class FakeRestTemplate extends RestTemplate {
        Object reply;
        RuntimeException toThrow;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
            if (toThrow != null) {
                throw toThrow;
            }
            return (T) reply;
        }
    }

    private FakeAuditLogService auditLog;
    private FakeRestTemplate restTemplate;
    private AiValidationService service;

    @BeforeEach
    void setUp() {
        auditLog = new FakeAuditLogService();
        restTemplate = new FakeRestTemplate();
        service = new AiValidationService(auditLog, restTemplate);
        ReflectionTestUtils.setField(service, "aiModel", "test-model");
        ReflectionTestUtils.setField(service, "aiTimeoutMs", 5000L);
        useHeuristic();
    }

    private Wallet wallet(String name, String purpose) {
        FairPayGroup group = new FairPayGroup();
        group.setId(UUID.randomUUID());
        group.setName(name + " Group");

        Wallet w = new Wallet();
        w.setId(UUID.randomUUID());
        w.setName(name);
        w.setPurpose(purpose);
        w.setGroup(group);
        w.setAutoApproveThreshold(100.0);
        w.setBudgetLimit(1000.0);
        return w;
    }


    private void useHeuristic() {
        ReflectionTestUtils.setField(service, "aiEnabled", false);
        ReflectionTestUtils.setField(service, "aiEndpoint", "");
        ReflectionTestUtils.setField(service, "aiApiKey", "");
    }


    private void useExternalAi() {
        ReflectionTestUtils.setField(service, "aiEnabled", true);
        ReflectionTestUtils.setField(service, "aiEndpoint", "https://ai.example/v1/chat/completions");
        ReflectionTestUtils.setField(service, "aiApiKey", "test-key");
    }

    private boolean validate(EvalCase c) {
        return service.validateTransaction(
                wallet(c.walletName(), c.walletPurpose()),
                c.merchant(), c.category(), c.amount(), "evaluator@test.com");
    }

    record EvalCase(String walletName, String walletPurpose, String merchant, String category,
                    double amount, boolean expectedValid, String note) {
        @Override
        public String toString() {
            return String.format("[%s] %s @ %s/%s -> expect %s",
                    note, walletName, merchant, category, expectedValid ? "VALID" : "MISMATCH");
        }
    }

  
    static Stream<EvalCase> heuristicContract() {
        return Stream.of(
                // R1 — grocery/supermarket on a travel/flight wallet -> MISMATCH
                new EvalCase("Travel Fund", "Holiday travel", "Lidl", "Groceries", 42.0, false, "R1-cat-grocery"),
                new EvalCase("Travel Fund", "Holiday travel", "Walmart", "Food", 30.0, false, "R1-merchant-walmart"),
                new EvalCase("Flight Pool", "Flight tickets", "Coop", "Shopping", 15.0, false, "R1-merchant-coop"),
                new EvalCase("Travel Fund", "travel", "Local Supermarket", "Daily", 20.0, false, "R1-merchant-supermarket"),
                // R2 — electronics on a food/restaurant/groceries wallet -> MISMATCH
                new EvalCase("Food Budget", "Food and dining", "MediaMarkt", "Electronics", 500.0, false, "R2-food"),
                new EvalCase("Team Lunch", "restaurant outings", "BestBuy", "electronics", 800.0, false, "R2-restaurant"),
                new EvalCase("Groceries", "weekly grocer shopping", "Apple Store", "Electronics", 1200.0, false, "R2-grocer"),
                // R3 — entertainment on an office wallet -> MISMATCH
                new EvalCase("Office Supplies", "office expenses", "Cinema City", "Movies", 25.0, false, "R3-merchant-cinema"),
                new EvalCase("Office Supplies", "office expenses", "Local Store", "Entertainment", 25.0, false, "R3-cat-entertainment"),
                new EvalCase("Office Supplies", "office expenses", "Spotify", "Subscription", 10.0, false, "R3-merchant-spotify"),
                // Clear matches the heuristic must leave approved
                new EvalCase("Travel Fund", "Holiday travel", "Lufthansa", "Flight", 350.0, true, "OK-travel-flight"),
                new EvalCase("Food Budget", "Food and dining", "McDonalds", "Restaurant", 18.0, true, "OK-food-restaurant"),
                new EvalCase("Office Supplies", "office expenses", "Staples", "Office Supplies", 60.0, true, "OK-office-supplies"),
                new EvalCase("Groceries", "weekly grocer shopping", "Lidl", "Groceries", 55.0, true, "OK-grocer-grocery"),
                new EvalCase("Movie Night", "entertainment fund", "Netflix", "Streaming", 12.0, true, "OK-entertainment")
        );
    }

   
    static Stream<EvalCase> semanticDataset() {
        return Stream.of(
                // Correctly handled by the rules
                new EvalCase("Travel Fund", "Holiday travel", "Lidl", "Groceries", 42.0, false, "rule-hit"),
                new EvalCase("Food Budget", "Food and dining", "BestBuy", "Electronics", 700.0, false, "rule-hit"),
                new EvalCase("Office Supplies", "office expenses", "Cinema City", "Entertainment", 25.0, false, "rule-hit"),
                new EvalCase("Travel Fund", "Holiday travel", "Booking.com", "Hotel", 220.0, true, "true-match"),
                new EvalCase("Food Budget", "Food and dining", "Pizza Hut", "Restaurant", 35.0, true, "true-match"),
                new EvalCase("Office Supplies", "office expenses", "Staples", "Stationery", 40.0, true, "true-match"),
                // Blind spots — semantically a mismatch, but no rule matches => heuristic over-approves
                new EvalCase("Travel Fund", "Holiday travel", "Apple Store", "Electronics", 999.0, false, "blind-electronics-on-travel"),
                new EvalCase("Office Supplies", "office expenses", "Steam", "Gaming", 60.0, false, "blind-gaming-on-office"),
                new EvalCase("Food Budget", "Food and dining", "Shell", "Fuel", 80.0, false, "blind-fuel-on-food"),
                new EvalCase("Movie Night", "entertainment fund", "Pharmacy Plus", "Healthcare", 45.0, false, "blind-health-on-entertainment"),
                new EvalCase("Travel Fund", "Holiday travel", "Casino Royale", "Gambling", 500.0, false, "blind-gambling-on-travel"),
                // True matches that should stay approved
                new EvalCase("Travel Fund", "Holiday travel", "Uber", "Transport", 25.0, true, "true-match"),
                new EvalCase("Movie Night", "entertainment fund", "AMC Theatres", "Cinema", 30.0, true, "true-match"),
                new EvalCase("Food Budget", "Food and dining", "Starbucks", "Cafe", 9.0, true, "true-match")
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("heuristicContract")
    @DisplayName("Heuristic contract: each designed rule produces the exact expected verdict")
    void heuristicContractIsHonoured(EvalCase c) {
        assertThat(validate(c))
                .as("heuristic verdict for %s", c)
                .isEqualTo(c.expectedValid());
    }

    @Test
    @DisplayName("Heuristic is case-insensitive across name, purpose, merchant and category")
    void heuristicIsCaseInsensitive() {
        assertThat(service.validateTransaction(
                wallet("TRAVEL FUND", "HOLIDAY TRAVEL"), "LIDL", "GROCERIES", 10.0, "u@t.com"))
                .as("uppercase grocery-on-travel should still be flagged")
                .isFalse();
    }

    @Test
    @DisplayName("Heuristic tolerates null merchant/category/purpose without throwing")
    void heuristicHandlesNulls() {
        Wallet w = wallet("General", null);
        assertThat(service.validateTransaction(w, null, null, 10.0, "u@t.com"))
                .as("no rule matches null fields -> approve")
                .isTrue();
    }

    @Test
    @DisplayName("Every validation writes exactly one AI_DECISION audit entry")
    void everyDecisionIsAudited() {
        validate(new EvalCase("Travel Fund", "Holiday travel", "Lidl", "Groceries", 42.0, false, "x"));
        assertThat(auditLog.entries)
                .hasSize(1)
                .allMatch(e -> e.startsWith("AI_DECISION"));
    }

    @Test
    @DisplayName("getValidationReason matches the rule that fired")
    void validationReasonMatchesRule() {
        assertThat(service.getValidationReason(wallet("Travel Fund", "Holiday travel"), "Lidl", "Groceries"))
                .contains("Travel/Flight");
        assertThat(service.getValidationReason(wallet("Food Budget", "Food and dining"), "BestBuy", "Electronics"))
                .contains("Food/Restaurant/Groceries");
        assertThat(service.getValidationReason(wallet("Office Supplies", "office expenses"), "Cinema City", "Entertainment"))
                .contains("Office");
    }

  
    @Test
    @DisplayName("Semantic eval: score the heuristic against ground truth and report metrics")
    void semanticEvalReport() {
        List<EvalCase> cases = semanticDataset().toList();

       
        int tp = 0, tn = 0, fp = 0, fn = 0;
        List<String> mistakes = new ArrayList<>();

        for (EvalCase c : cases) {
            boolean predictedValid = validate(c);
            boolean goldValid = c.expectedValid();
            if (!goldValid && !predictedValid) {
                tp++;                      
            } else if (goldValid && predictedValid) {
                tn++;                     
            } else if (goldValid) {
                fp++;                    
                mistakes.add("FALSE-ALARM " + c);
            } else {
                fn++;                 
                mistakes.add("MISS        " + c);
            }
        }

        int total = cases.size();
        double accuracy = (double) (tp + tn) / total;
        double precision = tp + fp == 0 ? 1.0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 1.0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

        System.out.println("\n================ AI Transaction Validator — Semantic Eval ================");
        System.out.printf("Mode: HEURISTIC FALLBACK   Cases: %d%n", total);
        System.out.println("Confusion matrix (positive = MISMATCH detected):");
        System.out.printf("  TP=%d  FP=%d%n  FN=%d  TN=%d%n", tp, fp, fn, tn);
        System.out.printf("Accuracy=%.2f  Precision=%.2f  Recall=%.2f  F1=%.2f%n",
                accuracy, precision, recall, f1);
        if (!mistakes.isEmpty()) {
            System.out.println("Blind spots / mistakes:");
            mistakes.forEach(m -> System.out.println("  " + m));
        }
        System.out.println("==========================================================================\n");

        assertThat(fp)
                .as("heuristic produced false positives (wrongly declined valid transactions): see report")
                .isZero();

        assertThat(accuracy)
                .as("heuristic accuracy below floor: see report")
                .isGreaterThanOrEqualTo(0.60);
    }

 
    @Nested
    @DisplayName("External AI integration")
    class ExternalAi {

        private Map<String, Object> chatResponse(String content) {
            Map<String, Object> message = new HashMap<>();
            message.put("content", content);
            Map<String, Object> choice = new HashMap<>();
            choice.put("message", message);
            Map<String, Object> response = new HashMap<>();
            response.put("choices", List.of(choice));
            return response;
        }

        static Stream<Arguments> aiReplies() {
            return Stream.of(
                    Arguments.of("true", true),
                    Arguments.of("false", false),
                    Arguments.of("True.", true),
                    Arguments.of("  TRUE  ", true),
                    Arguments.of("no, false", false)
            );
        }

        @ParameterizedTest(name = "AI says \"{0}\" -> valid={1}")
        @MethodSource("aiReplies")
        @DisplayName("Boolean replies are parsed into the verdict")
        void parsesBooleanReplies(String content, boolean expected) {
            useExternalAi();
            restTemplate.reply = chatResponse(content);
            assertThat(service.validateTransaction(
                    wallet("Travel Fund", "Holiday travel"), "Lidl", "Groceries", 10.0, "u@t.com"))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("Null content falls back to approve")
        void weirdResponsesFallBackToApprove() {
            useExternalAi();
            restTemplate.reply = chatResponse(null);
            assertThat(service.validateTransaction(
                    wallet("Travel Fund", "Holiday travel"), "Lidl", "Groceries", 10.0, "u@t.com"))
                    .as("null AI content should fall back to approve")
                    .isTrue();
        }

        @Test
        @DisplayName("Upstream AI failure surfaces as TransactionValidationException")
        void aiFailureBecomesValidationException() {
            useExternalAi();
            restTemplate.toThrow = new RestClientException("upstream 503");

            Wallet w = wallet("Travel Fund", "Holiday travel");
            assertThatThrownBy(() -> service.validateTransaction(w, "Lidl", "Groceries", 10.0, "u@t.com"))
                    .isInstanceOf(AiValidationService.TransactionValidationException.class);
        }
    }
}
