package com.mdsproject.backend.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingSuggestionItemDto {
    private String productName;
    private String description;
    private Double estimatedPriceEur;
    private String sourceName;
    private String sourceUrl;
}
