package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PremiumBreakupDetails {

    private String totalPremium;
    private String totalTax;
    private List<CoverageDetail> coveragesList;
}
