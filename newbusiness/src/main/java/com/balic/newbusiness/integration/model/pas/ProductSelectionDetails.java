package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSelectionDetails {

    private String baseCoverageCode;
    private String branchCode;
    private String policyIssueState;
    private String productPlan;
}
