package com.mdsproject.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingSuggestionsResponse {
    private UUID walletId;
    private String walletName;
    private String walletPurpose;
    private Double budgetLimit;
    private Double spentAmount;
    private Double remainingBudget;
    private String summary;
    private List<ShoppingSuggestionItemDto> suggestions;
}
