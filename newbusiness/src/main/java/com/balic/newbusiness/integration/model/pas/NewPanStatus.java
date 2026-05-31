package com.balic.newbusiness.integration.model.pas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewPanStatus {
	
	@JsonProperty("ip_pan_status")
    private PanDetail ipPanStatus;
    @JsonProperty("ph_pan_status")
    private PanDetail phPanStatus;
    @JsonProperty("payer_pan_status")
    private PanDetail payerPanStatus;

}
