package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSelectionDetails {

    private String baseCoverageCode;
    private String branchCode;
    private String policyIssueState;
    private String productPlan;
}
