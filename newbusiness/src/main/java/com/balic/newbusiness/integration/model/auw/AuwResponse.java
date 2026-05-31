package com.balic.newbusiness.integration.model.auw;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuwResponse {

	@JsonProperty("status")
    private String status;
    @JsonProperty("status_code")
    private String statusCode;
    @JsonProperty("status_msg")
    private String statusMsg;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("tranx_response")
    private TranxResponse tranxResponse;

    @Data
    public static class TranxResponse {
        @JsonProperty("stp_Flag")
        private List<StpFlag> stpFlag;
        @JsonProperty("rule_details")
        private List<RuleDetail> ruleDetails;
    }

    @Data
    public static class StpFlag {
        @JsonProperty("flag")
        private String flag;
    }

    @Data
    public static class RuleDetail {
        @JsonProperty("rule_id")
        private String ruleId;
        @JsonProperty("rule_description")
        private String ruleDescription;
    }
}
