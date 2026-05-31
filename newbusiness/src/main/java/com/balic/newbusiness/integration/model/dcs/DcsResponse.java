package com.balic.newbusiness.integration.model.dcs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DcsResponse {
	
	@JsonProperty("status")
    private String status;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("tranx_detail_list")
    private List<TransactionDetail> tranxDetailList;

    @Data
    public static class TransactionDetail {

        @JsonProperty("id")
        private String id;

        @JsonProperty("desc")
        private String desc;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("short_desc")
        private String shortDesc;

        @JsonProperty("valid_period")
        private String validPeriod;

        @JsonProperty("category")
        private String category;

        @JsonProperty("role")
        private String role;
    }

}
