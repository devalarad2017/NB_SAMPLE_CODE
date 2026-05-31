package com.balic.newbusiness.integration.model.proposal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProposalRequest {
    private String activefunds;
    private AgentDetails agentDetails;
    private String applnNo;
    private AppointeeDetails appointeeDetails;
    private String backdatedDate;
    private BankPayerDetails bankPayerDetails;
    private List<FundsDetails> bondTargetFund;
    private List<ChildCareDetails> childCareDetails;
    private String creationDate;
    private String existingFamilyPolicy;
    private String familyBenefit;
    private FatcaDetails fatcaDetails;
    private List<FundsDetails> fundsDetails;
    private String iccrOtpGenerateDt;
    private String iccrOtpTimestamp;
    private String iccrOtpValidateDt;
    private String investStrategyCode;
    private IpCurrentAddress ipCurrentAddress;
    private IpDetails ipDetails;
    private IpFemaleDGHDetails ipFemaleDGHDetails;
    private IpGoodHealthdetails ipGoodHealthdetails;
    private IpInsuranceFamilyDetails ipInsuranceFamilyDetails;
    private IpKycAmlDocDetails ipKycAmlDocDetails;
    private IpLifeStyleDetails ipLifeStyleDetails;
    private IpPermanentAddress ipPermanentAddress;
    private IpPosgsDGHDto ipPosgsDGHDto;
    private String isPOS;
    private String issueLater;
    private List<FundsDetails> liquidTargetFund;
    private int nbId;
    private List<NomineeDetails> nomineeDetails;
    private String otpAcceptance;
    private PhAddress phCurrentAddress;
    private PhDetails phDetails;
    private PhFemaleDGHDetails phFemaleDGHDetails;
    private PhGoodHealthdetails phGoodHealthdetails;
    private PhInsuranceFamilyDetails phInsuranceFamilyDetails;
    private PhKycAmlDocDetails phKycAmlDocDetails;
    private PhLifeStyleDetails phLifeStyleDetails;
    private PhAddress phPermanentAddress;
    private PhPosgsDGHDto phPosgsDGHDto;
    private PremiumCollectionDetails premiumCollectionDetails;
    private PremiumDetails premiumDetails;
    private String productUin;
    private String proposalOtpGenerateDt;
    private String proposalOtpValidateDt;
    private String proposalOtptimestamp;
    private String relationship;
    private RiderDetails riderDetails;
    private List<FundsDetails> sourceFund;
    private TopUpPremiumDetails topUpPremiumDetails;

    @Data
    public static class AgentDetails {
        private String fscIcCode;
        private String icSmName;
        private String leadCode;
        private String proposalType;
        private String rtlnRefCode;
        private String spCode;
        private String spName;
        private String subIdCode;
        // Getters and setters
    }

    @Data
    public static class AppointeeDetails {
        private String apptDOB;
        private String apptFirstNm;
        private String apptFullName;
        private String apptLastNm;
        private String apptMiddleNm;
        private String apptRtlnNominee;
        // Getters and setters
    }

    @Data
    public static class BankPayerDetails {
        private String accountNo;
        private String accountType;
        private String applicantKnownMonths;
        private String applicantKnownYears;
        private String applicantRltdEmp;
        private String bankName;
        private String branchName;
        private String eiaNo;
        private String ifscCode;
        private String payerAge;
        private String payerArea;
        private String payerDistrict;
        private String payerDob;
        private String payerFirstName;
        private String payerFlatNo;
        private String payerGender;
        private String payerLandmark;
        private String payerLastName;
        private String payerPanNo;
        private String payerPremisesName;
        private String payerState;
        private String payerStreet;
        private String payerTown;
        private String payerType;
        private String paymentMode;
        private String premiumPaidBy;
        private String rtlnWtIp;
        // Getters and setters
    }

    @Data
    public static class FundsDetails {
        private String biNumber;
        private String createdBy;
        private String createdDt;
        private String fundCode;
        private int fundId;
        private String fundName;
        private int nbId;
        private int percentageInvested;
        private String updatedBy;
        private String updatedDt;
    }

    @Data
    public static class ChildCareDetails {
        private String beneficiaryName;
        private String bt;
        private String dob;
        private String gender;
        private String monthlyIncome;
        private String planOption;
        private String pt;
        private String relation;
        private String riderSumAssured;
    }

    @Data
    public static class FatcaDetails {
        private String countryAddress;
        private String countryName;
        private String holdMailInstruction;
        private String holdMailInstructionDetails;
        private String isdCode;
        private String landlineNumber;
        private String mobileNumer;
        private String powerOfAttorney;
        private String powerOfAttorneyAddress;
        private String powerOfAttorneyName;
        private String powerOfAttorneyNumber;
        private String residentOutsideIndia;
        private String standardDetails;
        private String standardInstruction;
        private String taxResidentOutsideIndia;
        private String telephoneNumberOutIndia;
        private String tinIssuingCountry;
        private String tinNumber;
    }

    @Data
    public static class IpCurrentAddress {
        private String addressType;
        private String area;
        private String buildingName;
        private String buildingNumber;
        private String city;
        private String co;
        private String country;
        private String ipPhType;
        private String landmark;
        private String perAddIsCommAdd;
        private String pincode;
        private String policeStation;
        private String state;
        private String streetName;
        private String town;
    }

    @Data
    public static class IpDetails {
        private String age;
        private String alternateMobileNo;
        private String annualIncome;
        private String countryOfBirth;
        private String countryOfResidence;
        private String dob;
        private String education;
        private String emailId;
        private String employer;
        private String employerAddress;
        private String employerContactNo;
        private String employerWebsite;
        private String facebookId;
        private String fatherName;
        private String firstName;
        private String gender;
        private String ipPhType;
        private String ipRelation;
        private String isHandicappedAdverse;
        private String lastName;
        private String lifeGoal;
        private String maritalStatus;
        private String middleName;
        private String mobileNo;
        private String motherName;
        private String nationality;
        private String natureOfDuty;
        private String occupation;
        private String pan;
        private String phoneNo;
        private String placeOfBirth;
        private String proceedWithForm60;
        private String purposeOfInsurance;
        private String residenceStatus;
        private String spouseName;
        private String suffix;
        private String title;
    }

    @Data
    public static class IpFemaleDGHDetails {

        private String areYouPregnant;
        private String description;
        private String haveAnyGynecologicalCompications;
        private String totalCoverageHusband;
    }

    @Data
    public static class IpGoodHealthdetails {
        private String anyInjuryDisorder;
        private String haveEverDiagnosed;
        private String isAsthma;
        private String isBloodDisorder;
        private String isBypassSurgery;
        private String isCancer;
        private String isChestPain;
        private String isDiabetes;
        private String isGenitourinary;
        private String isHIVPositive;
        private String isLiverDisorder;
        private String isPancreatitis;
        private String isPhysicalDeformity;
        private String isStrock;
    }

    @Data
    public static class IpInsuranceFamilyDetails {
        private String annualPremiumForDependents;
        private String countOfPolicies;
        private List<FamilyMember> familyDetails;
        private String isHistoryOFDiabetes;
        private String isLifeHealthInsuranceDeclined;
        private String isPolicaticalExposed;
        private String numberOfMemberDiagnosisTime;
        private String policaticalExposedDetails;
        private String totalSumAssured;
    }

    @Data
    public static class FamilyMember {
        private String age;
        private String ageAtDeath;
        private String causeOfDeath;
        private String healthStatus;
        private String memberName;
    }

    @Data
    public static class IpKycAmlDocDetails {
        private String addressProof;
        private String ageProof;
        private String identityProof;
        private String incomeProof;
        private String otherDocuments;
    }

    @Data
    public static class IpLifeStyleDetails {
        private String adventurousAvocation;
        private String causeOfWeightChange;
        private String changeInConsumption;
        private String convictedCourtLaw;
        private String dateOfQuit;
        private String frequencyOfConsumption;
        private String height;
        private String isConsumetobacco;
        private String isRegularConsumeAlcohol;
        private String isbodyWeightChanged;
        private String narcoticsTreatment;
        private String narcoticsTreatmentDetails;
        private String quanityPerDayOfTabbacco;
        private String quantityOfConsumption;
        private String tobaccoProduct;
        private String weight;
    }

    @Data
    public static class IpPermanentAddress {
        private String addressType;
        private String area;
        private String buildingName;
        private String buildingNumber;
        private String city;
        private String co;
        private String country;
        private String ipPhType;
        private String landmark;
        private String perAddIsCommAdd;
        private String pincode;
        private String policeStation;
        private String state;
        private String streetName;
        private String town;
    }

    @Data
    public static class IpPosgsDGHDto {
        private String dgh16;
        private String dgh17;
        private String dgh18;
        private String dgh19;
        private String dgh20;
        private String dgh21;
    }

    @Data
    public static class NomineeDetails {
        private String apptDOB;
        private String apptFirstNm;
        private String apptFullName;
        private String apptLastNm;
        private String apptMiddleNm;
        private String apptRtlnNominee;
        private String firstName;
        private String lastname;
        private String middleName;
        private String nomineeFullName;
        private String nomineeShare;
        private String nomineedob;
        private String rtlnNominee;
    }

    @Data
    public static class PhAddress {
        private String addressType;
        private String area;
        private String buildingName;
        private String buildingNumber;
        private String city;
        private String co;
        private String country;
        private String ipPhType;
        private String landmark;
        private String perAddIsCommAdd;
        private String pincode;
        private String policeStation;
        private String state;
        private String streetName;
        private String town;
    }

    @Data
    public static class PhDetails {
        private String age;
        private String alternateMobileNo;
        private String annualIncome;
        private String countryOfBirth;
        private String countryOfResidence;
        private String dob;
        private String education;
        private String emailId;
        private String employer;
        private String employerAddress;
        private String employerContactNo;
        private String employerWebsite;
        private String facebookId;
        private String fatherName;
        private String firstName;
        private String gender;
        private String ipPhType;
        private String ipRelation;
        private String isHandicappedAdverse;
        private String lastName;
        private String maritalStatus;
        private String middleName;
        private String mobileNo;
        private String motherName;
        private String nationality;
        private String natureOfDuty;
        private String occupation;
        private String pan;
        private String phoneNo;
        private String placeOfBirth;
        private String proceedWithForm60;
        private String purposeOfInsurance;
        private String residenceStatus;
        private String spouseName;
        private String suffix;
        private String title;
    }

    @Data
    public static class PhFemaleDGHDetails {
        private String areYouPregnant;
        private String description;
        private String haveAnyGynecologicalCompications;
        private String totalCoverageHusband;
    }

    @Data
    public static class PhGoodHealthdetails {
        private String anyInjuryDisorder;
        private String haveEverDiagnosed;
        private String isAsthma;
        private String isBloodDisorder;
        private String isBypassSurgery;
        private String isCancer;
        private String isChestPain;
        private String isDiabetes;
        private String isGenitourinary;
        private String isHIVPositive;
        private String isLiverDisorder;
        private String isPancreatitis;
        private String isPhysicalDeformity;
        private String isStrock;
    }

    @Data
    public static class PhInsuranceFamilyDetails {
        private String annualPremiumForDependents;
        private String countOfPolicies;
        private List<FamilyMember> familyDetails;
        private String isHistoryOFDiabetes;
        private String isLifeHealthInsuranceDeclined;
        private String isPolicaticalExposed;
        private String numberOfMemberDiagnosisTime;
        private String policaticalExposedDetails;
        private String totalSumAssured;
    }

    @Data
    public static class PhKycAmlDocDetails {
        private String addressProof;
        private String ageProof;
        private String identityProof;
        private String incomeProof;
        private String otherDocuments;
    }

    @Data
    public static class PhLifeStyleDetails {
        private String adventurousAvocation;
        private String causeOfWeightChange;
        private String changeInConsumption;
        private String convictedCourtLaw;
        private String dateOfQuit;
        private String frequencyOfConsumption;
        private String height;
        private String isConsumetobacco;
        private String isRegularConsumeAlcohol;
        private String isbodyWeightChanged;
        private String narcoticsTreatment;
        private String narcoticsTreatmentDetails;
        private String quanityPerDayOfTabbacco;
        private String quantityOfConsumption;
        private String tobaccoProduct;
        private String weight;
    }

    @Data
    public static class PhPosgsDGHDto {
        private String dgh16;
        private String dgh17;
        private String dgh18;
        private String dgh19;
        private String dgh20;
        private String dgh21;
    }

    @Data
    public static class PremiumCollectionDetails {
        private String proposalDeposit;
    }

    @Data
    public static class PremiumDetails {
        private String benefitCode;
        private Integer biId;
        private String defermentPeriod;
        private String deferredIncome;
        private String earlyIncome;
        private String frequency;
        private String goalProtectionBenefit;
        private String increasingIncome;
        private String investStrategy;
        private String mode;
        private String nbId;
        private String optedFund;
        private String optionVariant;
        private String policyTerm;
        private String premium;
        private Integer premiumBackUp;
        private String premiumTerm;
        private String productId;
        private String productMultiplier;
        private String productName;
        private String productType;
        private String proposalType;
        private String rop;
        private String spwPercentage;
        private String spwStartYear;
        private String sumAssured;
        private String wealth;
    }

    @Data
    public static class RiderDetails {
        private String accidentalDeathBenefit;
        private String accidentalPermanentTotal;
        private String adbRppt;
        private String adbRt;
        private String aprAdbRppt;
        private String aprAdbRt;
        private String aprAdbSA;
        private String aprAtpdRppt;
        private String aprAtpdRt;
        private String aprAtpdSA;
        private String atpdRppt;
        private String atpdRt;
        private String ciOption;
        private String ciRppt;
        private String ciRt;
        private String criticalIllnessBenefit;
        private String familyIncomeBenefit;
        private String wop;
    }

    @Data
    public static class TopUpPremiumDetails {
        private String applicationNo;
        private Integer id;
        private String isTopUp;
        private String moduleName;
        private Integer nbId;
        private String status;
        private String topUpMultiplier;
        private String topUpPremium;
        private String topUpPremiumRangeFrom;
        private String topUpPremiumRangeTo;
        private String topUpSumAssured;
    }

}
