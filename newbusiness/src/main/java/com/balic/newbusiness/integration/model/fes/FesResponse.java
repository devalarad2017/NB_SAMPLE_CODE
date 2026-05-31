package com.balic.newbusiness.integration.model.fes;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FesResponse {

	@JsonProperty("status")
    private String status;
    @JsonProperty("status_code")
    private String statusCode;
    @JsonProperty("status_msg")
    private String statusMsg;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("tranx_detail_list")
    private List<TranxDetailList> tranxDetailList;
    @Data
    public static class TranxDetailList {
        @JsonProperty("income_source")
        private String incomeSource;
        @JsonProperty("cover_eligible")
        private String coverEligible;
    }
}
