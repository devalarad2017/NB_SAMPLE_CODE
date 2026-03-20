package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverageDetail {

    private String benefitCode;
    private String sumAssured;
    private String coveragePremium;
    private PremiumBreakup premiumBreakup;
}
