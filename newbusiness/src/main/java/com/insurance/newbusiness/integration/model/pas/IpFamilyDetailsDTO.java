package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpFamilyDetailsDTO {

    private String familyAdverseHistory;
    private List<Object> familyStatusList;
    private Integer howManyFamilyMemberAgedBelow50;
    private String sendBIPropoasalFormForReview;
}
