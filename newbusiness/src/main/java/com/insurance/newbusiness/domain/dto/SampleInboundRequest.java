package com.insurance.newbusiness.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SampleInboundRequest {

    @JsonProperty("pInObj1_inout")
    private WeoRecStrings150 pInObj1Inout;

    @JsonProperty("pInObj2_inout")
    private WeoRecStrings150 pInObj2Inout;

    @JsonProperty("pInObj3_inout")
    private WeoRecStrings150 pInObj3Inout;

    @JsonProperty("pInList1_inout")
    private WeoRecStrings150List pInList1Inout;

    @JsonProperty("pInList4_inout")
    private WeoRecStrings150List pInList4Inout;

    @JsonProperty("pInList5_inout")
    private WeoRecStrings150List pInList5Inout;

    @JsonProperty("pInList6_inout")
    private WeoRecStrings150List pInList6Inout;
}
