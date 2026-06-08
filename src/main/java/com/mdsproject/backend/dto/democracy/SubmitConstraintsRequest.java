package com.mdsproject.backend.dto.democracy;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubmitConstraintsRequest {
    private String startDate;
    private String endDate;
    private String dealBreakers;
    private Double maxContribution;
}
