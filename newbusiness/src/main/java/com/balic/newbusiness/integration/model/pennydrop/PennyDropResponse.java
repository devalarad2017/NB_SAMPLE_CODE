package com.balic.newbusiness.integration.model.pennydrop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PennyDropResponse {

    private String status;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("acc_holder_name")
    private String accHolderName;
}
