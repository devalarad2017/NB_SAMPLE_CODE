package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopUpDetailsDTO {

    private String topUpMultiplier;
    private String topUpPremium;
    private String topUpSumAssured;
    private Boolean isTopUpRequested;
}
