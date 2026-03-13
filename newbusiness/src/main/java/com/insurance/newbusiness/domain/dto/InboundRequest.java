package com.insurance.newbusiness.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Insurance application request from external partner")
public class InboundRequest {

    @Schema(description = "Partner identifier", example = "PARTNER_A", required = true)
    private String partnerCode;

    @Schema(description = "600+ generic key-value params. Keys are stringval1..stringvalN",
            example = "{\"stringval1\": \"John\", \"stringval2\": \"Smith\", \"stringval3\": \"12/03/1985\"}")
    private Map<String, String> params;

    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String partnerCode) { this.partnerCode = partnerCode; }
    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }
}
