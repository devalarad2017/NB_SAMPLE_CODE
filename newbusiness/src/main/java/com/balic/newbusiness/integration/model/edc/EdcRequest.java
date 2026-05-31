package com.balic.newbusiness.integration.model.edc;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdcRequest {

	@JsonProperty("appNo")
    private String appNo;

    @JsonProperty("isBsoApplicable")
    private String isBsoApplicable;

    @JsonProperty("nbId")
    private Integer nbId;

    @JsonProperty("source")
    private String source;

    @JsonProperty("cibilRequestDto")
    private CibilRequestDto cibilRequestDto;

    @JsonProperty("drcRequest")
    private DrcRequest drcRequest;

    @JsonProperty("edcData")
    private EdcData edcData;

    @Data
    public static class CibilRequestDto {
    	@JsonProperty("basicDetailsDto")
        private List<BasicDetailsDto> basicDetailsDto;
        @JsonProperty("ipPhSame")
        private String ipPhSame;
        @JsonProperty("isMinorLife")
        private boolean isMinorLife;
        @JsonProperty("isWop")
        private boolean isWop;
    }

    @Data
    public static class BasicDetailsDto {
        private String applicantFirstName;
        private String applicantLastName;
        private String gender;
        private String applicantMiddleName;
        private String dateOfBirth;
        private String idNumber;
        private String idType;
        private String mobileNo;
        private String ipPhType;
        private String emailAddress;
        private String residenceType;
        private String addressType;
        private String addressLine1;
        private String addressLine2;
        private String addressLine3;
        private String city;
        private String pinCode;
        private Long amount;
        private String stateCode;
        private String gstStateCode;
    }

    @Data
    public static class DrcRequest {
    	@JsonProperty("source")
        private String source;
        @JsonProperty("sales_Channel")
        private String salesChannel;
        @JsonProperty("risk_Commencement_Date")
        private String riskCommencementDate;
        @JsonProperty("life_Assured_Client_ID")
        private String lifeAssuredClientId;
        @JsonProperty("life_Assured_ID_Proof")
        private String lifeAssuredIdProof;
        @JsonProperty("policy_Owner_Occupation")
        private String policyOwnerOccupation;
        @JsonProperty("policy_Owner_Qualification")
        private String policyOwnerQualification;
        @JsonProperty("base_Plan_Sum_Assured")
        private String basePlanSumAssured;
        @JsonProperty("policy_Owner_Pincode")
        private String policyOwnerPincode;
        @JsonProperty("policy_Owner_Country")
        private String policyOwnerCountry;
        @JsonProperty("policy_Owner_Industry")
        private String policyOwnerIndustry;
        @JsonProperty("policy_Issue_Date")
        private String policyIssueDate;
        @JsonProperty("product_Name")
        private String productName;
        @JsonProperty("policy_Owner_Gender")
        private String policyOwnerGender;
        @JsonProperty("agent_Type")
        private String agentType;
        @JsonProperty("policy_Owner_Client_ID")
        private String policyOwnerClientId;
        @JsonProperty("life_Assured_Qualification")
        private String lifeAssuredQualification;
        @JsonProperty("policy_Owner_Age_Proof")
        private String policyOwnerAgeProof;
        @JsonProperty("policy_Owner_ID_Proof")
        private String policyOwnerIdProof;
        @JsonProperty("policy_Owner_Age")
        private String policyOwnerAge;
        @JsonProperty("premium_Payment_Mode")
        private String premiumPaymentMode;
        @JsonProperty("life_Assured_DOB")
        private String lifeAssuredDob;
        @JsonProperty("life_Assured_State")
        private String lifeAssuredState;
        @JsonProperty("line_of_Business")
        private String lineOfBusiness;
        @JsonProperty("product_Code")
        private String productCode;
        @JsonProperty("contract_No")
        private String contractNo;
        @JsonProperty("life_Assured_Nationality")
        private String lifeAssuredNationality;
        @JsonProperty("branch_Location")
        private String branchLocation;
        @JsonProperty("advisor_Code")
        private String advisorCode;
        @JsonProperty("life_Assured_Gender")
        private String lifeAssuredGender;
        @JsonProperty("sum_Under_Consideration")
        private String sumUnderConsideration;
        @JsonProperty("policy_Owner_Marital_Status")
        private String policyOwnerMaritalStatus;
        @JsonProperty("policy_Owner_DOB")
        private String policyOwnerDob;
        @JsonProperty("zone_Location")
        private String zoneLocation;
        @JsonProperty("life_Assured_City")
        private String lifeAssuredCity;
        @JsonProperty("life_Assured_Industry")
        private String lifeAssuredIndustry;
        @JsonProperty("agent_Class")
        private String agentClass;
        @JsonProperty("billing_Frequency")
        private String billingFrequency;
        @JsonProperty("annual_Premium")
        private String annualPremium;
        @JsonProperty("policy_Owner_City")
        private String policyOwnerCity;
        @JsonProperty("agent_Code")
        private String agentCode;
        @JsonProperty("smoker_Flag")
        private String smokerFlag;
        @JsonProperty("life_Assured_Country")
        private String lifeAssuredCountry;
        @JsonProperty("life_Assured_Age")
        private String lifeAssuredAge;
        @JsonProperty("policy_Owner_Nationality")
        private String policyOwnerNationality;
        @JsonProperty("online_Offline_Flag")
        private String onlineOfflineFlag;
        @JsonProperty("policy_Owner_Annual_Income")
        private String policyOwnerAnnualIncome;
        @JsonProperty("nominee_Relation")
        private String nomineeRelation;
        @JsonProperty("life_Assured_Occupation")
        private String lifeAssuredOccupation;
        @JsonProperty("life_Assured_Marital_Status")
        private String lifeAssuredMaritalStatus;
        @JsonProperty("application_No")
        private String applicationNo;
        @JsonProperty("life_Assured_Pincode")
        private String lifeAssuredPincode;
        @JsonProperty("policy_Owner_Income_Proof")
        private String policyOwnerIncomeProof;
        @JsonProperty("life_Assured_Annual_Income")
        private String lifeAssuredAnnualIncome;
        @JsonProperty("premium_Payment_Term")
        private String premiumPaymentTerm;
        @JsonProperty("life_Assured_Income_Proof")
        private String lifeAssuredIncomeProof;
        @JsonProperty("life_Assured_Age_Proof")
        private String lifeAssuredAgeProof;
        @JsonProperty("policy_Term")
        private String policyTerm;
        @JsonProperty("Additional_Input")
        private AdditionalInput additionalInput;
    }
    
    @Data
    public static class AdditionalInput {
        @JsonProperty("Life_Assured_Name")
        private String lifeAssuredName;
        @JsonProperty("Life_Assured_Contact_No")
        private String lifeAssuredContactNo;
        @JsonProperty("Life_Assured_PAN_No")
        private String lifeAssuredPanNo;
        @JsonProperty("Life_Assured_Email_Id")
        private String lifeAssuredEmailId;
    }
    
    @Data
    public static class EdcData {
    	private String isSuperWoman;
        private String contractId;
        private String dateOfCommencement;
        private String emailValidity;
        private String formFillingMode;
        private String callSource;
        private String ipEmploymentStatus;
        private String irdaChannel;
        private String lifeStage;
        private String negativeAgencyPinCode;
        private String negativePinCode;
        private String nsdlPanResponse;
        private String phEmploymentStatus;
        private String productUIN;
        private String stmStartDate;
        private String deviceCode;
        private String deviceOS;
        private String deviceType;
        private String phoneMake;
        private String appNo;
        private String agentClubStatus;
        private String agentCode;
        private String branchCode;
        private String ckycMatch;
        private String ekycMatch;
        private String fpPayMode;
        private String proposalNo;
        private String stmCode;
        private String agentStartDate;
        private String channel;
        private String distributionChannel;
        private String subChannel1;
        private String annualPremium;
        private String bookingFrequency;
        private String policyTerm;
        private String premiumTerm;
        private String productId;
        private String isAxisBurgundyCustomer;
        private String sumAssured;
        private String coverCode;
        private String phDob;
        private String phEducation;
        private String phEmail;
        private String phGender;
        private String phIncome;
        private String phMaritalStatus;
        private String phMobile;
        private String phName;
        private String phFacebookId;
        private String phOccupation;
        private String phResidenceCountry;
        private String phNRIFlag;
        private String laFirstName;
        private String laMiddleName;
        private String laLastName;
        private String ipDob;
        private String ipEducation;
        private String ipEmail;
        private String ipGender;
        private String ipIncome;
        private String ipMaritalStatus;
        private String ipMobile;
        private String ipName;
        private String ipOccupation;
        private String ipResidenceCountry;
        private String lifeGoal;
        private String panNo;
        private String panVerificationStatus;
        private String purposeOfInvestment;
        private String ipNRIFlag;
        private String cpId;
        private String ipAddress;
        private String ipCity;
        private String ipCountry;
        private String ipPinCode;
        private String ipState;
        private String mailingAddress;
        private String mailingCity;
        private String mailingPincode;
        private String mailingState;
        private String phAddress;
        private String phCity;
        private String phCountry;
        private String phPinCode;
        private String phState;
        private String nominee4Dob;
        private String nominee4Relation;
        private String nominee4Share;
        private String nominee3Dob;
        private String nominee3Relation;
        private String nominee3Share;
        private String nominee2Dob;
        private String nominee2Relation;
        private String nominee2Share;
        private String nomineeDob;
        private String nomineeRelation;
        private String nomineeShare;
        private String appointeeDob;
        private String appointeeName;
        private String appointeeRelation;
        private String autoPayStatus;
        private String ipHeight;
        private String ipWeight;
        private String smoker;
        private String rider5AnnualPremium;
        private String rider5Code;
        private String rider5SA;
        private String rider4AnnualPremium;
        private String rider4Code;
        private String rider4SA;
        private String rider3AnnualPremium;
        private String rider3Code;
        private String rider3SA;
        private String rider2AnnualPremium;
        private String rider2Code;
        private String rider2SA;
        private String riderAnnualPremium;
        private String riderCode;
        private String riderSA;
        private String journeyPaymentType;
        private String edcFirstOrRepeatCall;
        private String newField11;
        private String newField13;
        private String newField14;
        private String newField16;
        private String newField17;
        private String newField18;
        private String newField15;
        private String newField19;
        private String newField20;
        private String newField24;
        private String newField22;
        private String newField21;
        private String newField9;
        private String newField23;
        private String newField25;
        private String newField52;
        private String subChannel2;
        private String appgraphy1;
        private String appgraphy2;
        private String xmlFile;
        private String newField1;
        private String newField54;
        private String newField55;
        private String newField34;
    }

    @Data
    public static class Rider5Code {
        @JsonProperty("PH_FName")
        private String phFname;
        @JsonProperty("PH_Mname") 
        private String phMname;
        @JsonProperty("PH_Lname") 
        private String phLname;
        @JsonProperty("PH_KYC_FName") 
        private String phKycFname;
        @JsonProperty("PH_KYC_Mname") 
        private String phKycMname;
        @JsonProperty("PH_KYC_Lname") 
        private String phKycLname;
        @JsonProperty("Cust_DOB") 
        private String custDob;
        @JsonProperty("DOB") 
        private String dob;
        @JsonProperty("Gender") 
        private String gender;
        @JsonProperty("MobileNo") 
        private String mobileNo;
        @JsonProperty("EmailID") 
        private String emailId;
        @JsonProperty("PH_Permenant_address") 
        private String phPermanentAddress;
        @JsonProperty("PH_communication_address") 
        private String phCommunicationAddress;
        @JsonProperty("PH_PerAdd_Pincode") 
        private String phPerAddPincode;
        @JsonProperty("PH_ComAdd_Pincode") 
        private String phComAddPincode;
        @JsonProperty("ExistingLifeInsuranceCover") 
        private String existingLifeInsuranceCover;
        @JsonProperty("Education") 
        private String education;
        @JsonProperty("Occupation") 
        private String occupation;
        @JsonProperty("AnnualIncome") 
        private String annualIncome;
    }

    @Data
    public static class Rider5SA {
        @JsonProperty("PH_FName") 
        private String phFname;
        @JsonProperty("PH_Mname") 
        private String phMname;
        @JsonProperty("PH_Lname") 
        private String phLname;
        @JsonProperty("PH_KYC_FName") 
        private String phKycFname;
        @JsonProperty("PH_KYC_Mname") 
        private String phKycMname;
        @JsonProperty("PH_KYC_Lname") 
        private String phKycLname;
        @JsonProperty("Cust_DOB") 
        private String custDob;
        @JsonProperty("DOB") 
        private String dob;
        @JsonProperty("Gender") 
        private String gender;
        @JsonProperty("MobileNo") 
        private String mobileNo;
        @JsonProperty("EmailID") 
        private String emailId;
        @JsonProperty("PH_Permenant_address") 
        private String phPermanentAddress;
        @JsonProperty("PH_communication_address") 
        private String phCommunicationAddress;
        @JsonProperty("PH_PerAdd_Pincode") 
        private String phPerAddPincode;
        @JsonProperty("PH_ComAdd_Pincode") 
        private String phComAddPincode;
        @JsonProperty("ExistingLifeInsuranceCover") 
        private String existingLifeInsuranceCover;
        @JsonProperty("Education") 
        private String education;
        @JsonProperty("Occupation") 
        private String occupation;
        @JsonProperty("AnnualIncome") 
        private String annualIncome;
    }
}

