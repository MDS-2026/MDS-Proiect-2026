package com.mdsproject.backend.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AI service: shopping ideas aligned with wallet purpose and budget.
 */
public interface ShoppingSuggestionAgent {

    @SystemMessage("""
            You are a shopping advisor for a shared group wallet app.
            Suggest realistic products to buy online that match the wallet purpose.
            Every item must fit within the remaining budget (sum of item prices <= remaining budget).
            Prefer well-known retailers (e.g. Amazon, eMAG, Altex, Decathlon, Booking.com) when relevant.
            Return ONLY valid JSON, no markdown, with this exact shape:
            {
              "summary": "one short sentence in Romanian or English",
              "suggestions": [
                {
                  "productName": "string",
                  "description": "string",
                  "estimatedPriceEur": 0.0,
                  "sourceName": "retailer or site name",
                  "sourceUrl": "https://full-url-to-product-or-search"
                }
              ]
            }
            Provide 3 to 5 suggestions. sourceUrl must be a plausible HTTPS link to find the offer.
            """)
    @UserMessage("""
            Wallet name: {{walletName}}
            Wallet purpose: {{walletPurpose}}
            Total budget (EUR): {{budgetLimit}}
            Already spent (EUR): {{spentAmount}}
            Remaining budget (EUR): {{remainingBudget}}
            Group context: {{groupName}}

            Generate shopping suggestions as JSON only.
            """)
    String generateSuggestionsJson(
            @dev.langchain4j.service.V("walletName") String walletName,
            @dev.langchain4j.service.V("walletPurpose") String walletPurpose,
            @dev.langchain4j.service.V("budgetLimit") double budgetLimit,
            @dev.langchain4j.service.V("spentAmount") double spentAmount,
            @dev.langchain4j.service.V("remainingBudget") double remainingBudget,
            @dev.langchain4j.service.V("groupName") String groupName
    );
}
