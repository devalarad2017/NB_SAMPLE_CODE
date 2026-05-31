package com.balic.newbusiness.integration.model.receipting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptingRequest {
	
	private String accountCode;
    private String accountType;
    private String agentCode;
    private String authCode;
    private String bankBranchCode;
    private Long cardNo; // Handled as Long for the 16-digit numeric value
    private String cardType;
    private String ctsNonCts;
    private String customerAccountNo;
    private String customerBankIfscCode;
    private String depositBankCode;
    private String depositBankName;
    private String draweeBankName;
    private String emailID;
    private String gatewayMessage;
    private String holderName;
    private String ifscCode;
    private String instrumentNO;
    private String issuingBankName;
    private String merchantNo;
    private String micrCode;
    private String mobileNo;
    private String paymentSource;
    private String renterInstrumentNO;
    private String utrNO;
    private String onlinePayMode;
    private String premiumType;
    private String applicationNo;
    private String policyNo;
    private String transactionAmount;
    private String balicTransactionId;
    private String paymentChannel;
    private String processedBy;
    private String tpslId;
    private String paymentGatewayID;
    private String bankTransactionId;
    private String topUpProposal;
    private Double topUpAmount;
    private String uniqueId;
    private String nbPayMode;
    private Long nbId;

}
