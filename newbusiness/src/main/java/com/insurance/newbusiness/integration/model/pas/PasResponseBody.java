package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasResponseBody {

    private Long applicationId;
    private String proposalNumber;
    private Boolean backdatedPolicyAllowed;
    private List<ChapterDetail> chapterDetails;
    private PolicyCheckIn policyCheckIn;
    private String status;
}
