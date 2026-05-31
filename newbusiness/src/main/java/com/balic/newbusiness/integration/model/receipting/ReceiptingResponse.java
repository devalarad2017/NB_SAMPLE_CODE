package com.balic.newbusiness.integration.model.receipting;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptingResponse {

	private String status;
    private String receiptNo;
}
