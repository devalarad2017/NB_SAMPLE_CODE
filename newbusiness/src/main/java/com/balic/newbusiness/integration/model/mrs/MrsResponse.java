package com.balic.newbusiness.integration.model.mrs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MrsResponse {

	@JsonProperty("status")
    private String status;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("status_msg")
    private String statusMsg;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("tranx_detail_list")
    private List<MedicalDetail> medicalDetailList;

    @Data
    public static class MedicalDetail {

        @JsonProperty("customer_id")
        private String customerId;

        @JsonProperty("medical_flag")
        private String medicalFlag;

        @JsonProperty("package_code")
        private String packageCode;

        @JsonProperty("report_name")
        private String reportName;
    }
}
