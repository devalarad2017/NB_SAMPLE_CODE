package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor 
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class PasRequestBody {

    private Long applicationId;
    private String proposalNumber;
    private Boolean backdatedPolicyAllowed;
    private List<ChapterDetail> chapterDetails;
    private PolicyCheckIn policyCheckIn;
    private String status;
}
