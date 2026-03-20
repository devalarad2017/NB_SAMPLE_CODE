package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportantDates {

    private String policyCheckInDate;
    private String policyIssueDate;
    private String policySignedDate;
    private String policyReceivedDate;
    private String policyYearDate;
    private String riskEffectiveDate;
}
