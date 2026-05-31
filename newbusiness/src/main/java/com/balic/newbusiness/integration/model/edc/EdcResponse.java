package com.balic.newbusiness.integration.model.edc;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdcResponse {

	@JsonProperty("appNo")
    private String appNo;

    @JsonProperty("edcScore")
    private String edcScore;

    @JsonProperty("edcAlert")
    private String edcAlert;

    @JsonProperty("alertReason")
    private String alertReason;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("tmpStmp")
    private String tmpStmp;

    @JsonProperty("modelAlert")
    private String modelAlert;

    @JsonProperty("persistencyScore")
    private String persistencyScore;

    @JsonProperty("persitencyAlert")
    private String persitencyAlert;

    @JsonProperty("persistencyRemarks")
    private String persistencyRemarks;

    @JsonProperty("productReco")
    private String productReco;

    @JsonProperty("ticketSizeReco")
    private String ticketSizeReco;

    @JsonProperty("qualityScore")
    private String qualityScore;

    @JsonProperty("qualityAlert")
    private String qualityAlert;

    @JsonProperty("qualityRemarks")
    private String qualityRemarks;

    @JsonProperty("incomeSegment")
    private String incomeSegment;

    @JsonProperty("digitalProfileScore")
    private String digitalProfileScore;

    @JsonProperty("digitalProfileAlert")
    private String digitalProfileAlert;

    @JsonProperty("bureauMatch")
    private String bureauMatch;

    @JsonProperty("bureauScore")
    private String bureauScore;

    @JsonProperty("bureauProfile")
    private String bureauProfile;

    @JsonProperty("edcSegment")
    private String edcSegment;

    @JsonProperty("iibPrismScore")
    private String iibPrismScore;

    @JsonProperty("partnerSegment")
    private String partnerSegment;

    @JsonProperty("edcModuleDecision")
    private String edcModuleDecision;

    @JsonProperty("decisionReason")
    private String decisionReason;

    @JsonProperty("retentionFlag")
    private String retentionFlag;

    @JsonProperty("retainableSa")
    private String retainableSa;

    @JsonProperty("phId")
    private String phId;

    @JsonProperty("tasa")
    private String tasa;

    @JsonProperty("existingRetention")
    private String existingRetention;

    @JsonProperty("existingReinsuredSaReinsure1")
    private String existingReinsuredSaReinsure1;

    @JsonProperty("existingReinsuredSaReinsure2")
    private String existingReinsuredSaReinsure2;

    @JsonProperty("existingReinsuredSaReinsure3")
    private String existingReinsuredSaReinsure3;

    @JsonProperty("existingReinsuredSaReinsure4")
    private String existingReinsuredSaReinsure4;

    @JsonProperty("existingTasaTerm")
    private String existingTasaTerm;

    @JsonProperty("newTasaTerm")
    private String newTasaTerm;

    @JsonProperty("existingTasaNonTerm")
    private String existingTasaNonTerm;

    @JsonProperty("totalRetained")
    private String totalRetained;

    @JsonProperty("rejetionDecision")
    private String rejetionDecision;

    @JsonProperty("issuanceProbability")
    private String issuanceProbability;

    @JsonProperty("reco1")
    private String reco1;

    @JsonProperty("reco2")
    private String reco2;

    @JsonProperty("reco3")
    private String reco3;

    @JsonProperty("reco4")
    private String reco4;

    @JsonProperty("reco5")
    private String reco5;

    @JsonProperty("reco6")
    private String reco6;

    @JsonProperty("reco7")
    private String reco7;

    @JsonProperty("reco8")
    private String reco8;

    @JsonProperty("reco9")
    private String reco9;

    @JsonProperty("reco10")
    private String reco10;

    @JsonProperty("qualityAlertReason")
    private String qualityAlertReason;

    @JsonProperty("qualityErrorCode")
    private String qualityErrorCode;

    @JsonProperty("qualityModelAlert")
    private String qualityModelAlert;

    @JsonProperty("qualitySegment")
    private String qualitySegment;

    @JsonProperty("persistencyAlertReason")
    private String persistencyAlertReason;

    @JsonProperty("persistencyErrorCode")
    private String persistencyErrorCode;

    @JsonProperty("persistencyModelAlert")
    private String persistencyModelAlert;

    @JsonProperty("persistencySegment")
    private String persistencySegment;

    @JsonProperty("partnerSegmentFeedback")
    private String partnerSegmentFeedback;

    @JsonProperty("incomeEstimated")
    private String incomeEstimated;

    @JsonProperty("incomeErrorCode")
    private String incomeErrorCode;

    @JsonProperty("incomeInconsistencyAlert")
    private String incomeInconsistencyAlert;

    @JsonProperty("incomeInconsistencyScore")
    private String incomeInconsistencyScore;

    @JsonProperty("incomeProofRequiredFlag")
    private String incomeProofRequiredFlag;

    @JsonProperty("incomeRemarks")
    private String incomeRemarks;

    @JsonProperty("edcAlertReason")
    private String edcAlertReason;

    @JsonProperty("addressProofRequiredFlag")
    private String addressProofRequiredFlag;

    @JsonProperty("ageProofRequiredFlag")
    private String ageProofRequiredFlag;

    @JsonProperty("idProofRequiredFlag")
    private String idProofRequiredFlag;

    @JsonProperty("panProofRequiredFlag")
    private String panProofRequiredFlag;

    @JsonProperty("followupAction")
    private String followupAction;

    @JsonProperty("recoUwJourney")
    private String recoUwJourney;

    @JsonProperty("iibSumAssured")
    private String iibSumAssured;

    @JsonProperty("placeeholder1")
    private String placeeholder1;

    @JsonProperty("placeholder2")
    private String placeholder2;

    @JsonProperty("placeholder3")
    private String placeholder3;

    @JsonProperty("placeholder4")
    private String placeholder4;

    @JsonProperty("placeholder5")
    private String placeholder5;

    @JsonProperty("placeholder6")
    private String placeholder6;

    @JsonProperty("placeholder7")
    private String placeholder7;

    @JsonProperty("placeholder8")
    private String placeholder8;

    @JsonProperty("placeholder9")
    private String placeholder9;

    @JsonProperty("placeholder10")
    private String placeholder10;

    @JsonProperty("isHighRisk")
    private String isHighRisk;

    @JsonProperty("actionCodeCategory")
    private String actionCodeCategory;

    @JsonProperty("drcCateory")
    private String drcCateory;

    @JsonProperty("pivcIndicator")
    private String pivcIndicator;

}
