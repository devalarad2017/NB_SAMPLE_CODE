package com.insurance.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IccrDTO {

    private String address;
    private Integer age;
    private String agentDetails;
    private String channel;
    private String fscOrIcCode;
    private String fscOrIcName;
    private String fscOrIcClub;
    private String guidelines;
    private String handicappedDescription;
    private String howlongYouKnowLA;
    private String id;
    private String income;
    private String isHandicaped;
    private String isRelatedOrEmpOfBalic;
    private String leadByCode;
    private String nameOfLA;
    private String occupation;
    private String others;
    private String proposalAcceptance;
    private String proposedInsured;
    private String relationshipRefCode;
    private String riskAssociated;
    private String spCode;
    private String spName;
    private String subIdCode;
    private Object sumAssured;
    private String validDataEntered;
    private String empCode;
    private String rmContact;
    private String rmEmail;
    private String rmName;
    private String distributionChannel;
    private String partnerId;
    private String accountType;
    private String trackId;
    private String subVertical;
    private String bsoCode;
    private String dohCode;
    private String bsoName;
    private String stmCode;
    private String isPOS;
    private String isBsoBypassed;
    private String bsoChannel;
    private String duid;
    private String issueLater;
    private String cisNumber;
    private String commissionType;
    private String issuanceGridApplicable;
    private String byPassFlag;
    private String agentsClub;
    private String vertical;
    private String isCommissionPayable;
}
