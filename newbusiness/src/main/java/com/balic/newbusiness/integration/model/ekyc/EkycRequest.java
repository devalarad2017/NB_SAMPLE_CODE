package com.balic.newbusiness.integration.model.ekyc;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EkycRequest {

	private String applicationNo;
}
