package com.balic.newbusiness.integration.model.ucs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class UcsResponse {
    @JsonProperty("TranxStatus")
    private TranxStatus tranxStatus;

    @JsonProperty("TranxResponse")
    private TranxResponse tranxResponse;

    @JsonProperty("cp_id_dedupe_data")
    private CpIdDedupeData cpIdDedupeData;

    @Data
    public static class TranxStatus {
        @JsonProperty("Status")
        private String status;
        @JsonProperty("Error_Description")
        private String errorDescription;
        @JsonProperty("Error_Code")
        private String errorCode;
        @JsonProperty("TransactionID")
        private String transactionId;
    }

    @Data
    public static class TranxResponse {
        @JsonProperty("Singlelife")
        private List<LifeResponse> singlelife;
        @JsonProperty("Customers")
        private List<LifeResponse> customers;
    }

    @Data
    public static class LifeResponse {
    	@JsonProperty("SUC")
        private Double suc; // Scientific notation: 5.43174992E8
        @JsonProperty("TAP")
        private Double tap; // Decimal: 250521.16
        @JsonProperty("APTPDTASA")
        private Double aptpdtasa;
        @JsonProperty("ADBSUC")
        private Double adbsuc;
        @JsonProperty("CIBTASA")
        private Double cibtasa;
        @JsonProperty("BDB_POSSUC")
        private Double bdbPossuc;
        @JsonProperty("APTPDSUC")
        private Double aptpdsuc;
        @JsonProperty("BDB_POSTASA")
        private Double bdbPostasa;
        @JsonProperty("LifeSUC")
        private Double lifeSuc; 
        @JsonProperty("LifeTASA")
        private Double lifeTasa;
        @JsonProperty("TASA")
        private Double tasa;
        @JsonProperty("CIBSUC")
        private Double cibsuc;
        @JsonProperty("ADBTASA")
        private Double adbtasa;
    }

    @Data
    public static class CpIdDedupeData {
        @JsonProperty("response_code")
        private String responseCode;
        @JsonProperty("response_message")
        private String responseMessage;
        @JsonProperty("input_application_no")
        private String inputApplicationNo;
        @JsonProperty("input_ip_type")
        private String inputIpType;
        @JsonProperty("status")
        private String status;
        @JsonProperty("matched_data")
        private List<MatchedData> matchedData;
    }
    
    @Data
    public static class MatchedData {
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
        
        private String gender;
        private String dob;
        
        @JsonProperty("address_line1")
        private String addressLine1;
        
        @JsonProperty("address_line2")
        private String addressLine2;
        
        @JsonProperty("address_line3")
        private String addressLine3;
        
        private String city;
        private String state;
        private String country;
        private String pincode;
        private String mobile1;
        private String mobile2;
        private String telephone1;
        private String telephone2;
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
        
        @JsonProperty("match_percentage")
        private String matchPercentage;
        
        @JsonProperty("rule_name")
        private String ruleName;
        
        @JsonProperty("bank_account1")
        private String bankAccount1;
        
        @JsonProperty("bank_account2")
        private String bankAccount2;
    }
}

