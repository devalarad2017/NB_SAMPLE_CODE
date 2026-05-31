package com.balic.newbusiness.integration.model.ucs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UcsApiRequest {
    @JsonProperty("request_source")
    private String requestSource;

    @JsonProperty("Proposal_number")
    private String proposalNumber;

    @JsonProperty("proposal_details")
    private List<ProposalDetail> proposalDetails;

    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;

    @Data
    public static class ProposalDetail {
        @JsonProperty("policy_number")
        private String policyNumber;
        @JsonProperty("customer_id")
        private String customerId;
        @JsonProperty("benefit_code")
        private String benefitCode;
        @JsonProperty("annual_premium")
        private String annualPremium;
        @JsonProperty("ppt")
        private String ppt;
        @JsonProperty("total_installment_premium")
        private String totalInstallmentPremium;
        @JsonProperty("benefit_sa")
        private String benefitSa;
        @JsonProperty("fund_value")
        private String fundValue;
        @JsonProperty("single_premium")
        private String singlePremium;
        @JsonProperty("term")
        private String term;
        @JsonProperty("payment_mode")
        private String paymentMode;
        @JsonProperty("benefit_installment_premium")
        private String benefitInstallmentPremium;
        @JsonProperty("benefit_status")
        private String benefitStatus;
        @JsonProperty("benefit_term")
        private String benefitTerm;
        @JsonProperty("doef")
        private String doef;
        @JsonProperty("GMB")
        private String gmb;
        @JsonProperty("installments_paid")
        private String installmentsPaid;
    }

    @Data
    public static class CustomerDetails {
        @JsonProperty("application_no")
        private String applicationNo;
        @JsonProperty("policy_ref")
        private String policyRef;
        @JsonProperty("ip_type")
        private String ipType;
        @JsonProperty("part_id")
        private String partId;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("middle_name")
        private String middleName;
        @JsonProperty("last_name")
        private String lastName;
        @JsonProperty("gender")
        private String gender;
        @JsonProperty("dob")
        private String dob;
        @JsonProperty("address_line1")
        private String addressLine1;
        @JsonProperty("address_line2")
        private String addressLine2;
        @JsonProperty("address_line3")
        private String addressLine3;
        @JsonProperty("city")
        private String city;
        @JsonProperty("state")
        private String state;
        @JsonProperty("country")
        private String country;
        @JsonProperty("pincode")
        private String pincode;
        @JsonProperty("mobile1")
        private String mobile1;
        @JsonProperty("mobile2")
        private String mobile2;
        @JsonProperty("telephone1")
        private String telephone1;
        @JsonProperty("telephone2")
        private String telephone2;
        @JsonProperty("email")
        private String email;
        @JsonProperty("pan_number")
        private String panNumber;
        @JsonProperty("aadhaar_no")
        private String aadhaarNo;
        @JsonProperty("father_name")
        private String fatherName;
        @JsonProperty("mother_name")
        private String motherName;
        @JsonProperty("ckyc_id")
        private String ckycId;
        @JsonProperty("virtual_id")
        private String virtualId;
        @JsonProperty("passport_id")
        private String passportId;
        @JsonProperty("voter_id")
        private String voterId;
        @JsonProperty("create_user")
        private String createUser;
        @JsonProperty("create_date")
        private String createDate;
        @JsonProperty("bank_account1")
        private String bankAccount1;
        @JsonProperty("bank_account2")
        private String bankAccount2;
    }
}

