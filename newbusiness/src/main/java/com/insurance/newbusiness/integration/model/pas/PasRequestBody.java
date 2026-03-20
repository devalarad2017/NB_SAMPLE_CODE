package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasRequestBody {

    private Long applicationId;
    private String proposalNumber;
    private List<ChapterDetail> chapterDetails;
    private PolicyCheckIn policyCheckIn;
    private String status;
}
