package com.balic.newbusiness.integration.model.pennydrop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PennyDropRequest {

    @JsonProperty("request_source")
    private String requestSource;

    private String policyNo;

    @JsonProperty("mobile_no")
    private String mobileNo;

    @JsonProperty("email_id")
    private String emailId;

    @JsonProperty("account_no")
    private String accountNo;

    @JsonProperty("ifsc_code")
    private String ifscCode;

    @JsonProperty("account_holder")
    private String accountHolder;

    @JsonProperty("cp_id")
    private String cpId;

    private String description;


}
