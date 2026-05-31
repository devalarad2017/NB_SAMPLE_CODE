package com.balic.newbusiness.service;

import com.balic.newbusiness.domain.dto.InboundReceiptingRequest;
import com.balic.newbusiness.domain.dto.ReceiptingResponse;
import com.balic.newbusiness.integration.client.ReceiptingApiClient;
import com.balic.newbusiness.integration.model.pas.ChapterDetail;
import com.balic.newbusiness.integration.model.pas.PasRequest;
import com.balic.newbusiness.integration.model.pas.PasRequestBody;
import com.balic.newbusiness.integration.model.pas.PasResponse;
import com.balic.newbusiness.integration.model.receipting.ReceiptingRequest;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;


@Service
@RequiredArgsConstructor
public class ReceiptingService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptingService.class);

    private final ObjectMapper objectMapper;
    private final ReceiptingApiClient receiptingApiClient;
    private final JourneyTrackingService trackingService;
    private final ReceiptingApiClient receiptingApiClient1;

    public ReceiptingResponse receiveAndAcknowledge(InboundReceiptingRequest receiptingRequest) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        log.info("[{}] Inbound request received | partner={}",
                correlationId, receiptingRequest.getPartnerCode());
        long journeyStart = System.currentTimeMillis();

        ReceiptingResponse receiptingResponse = new ReceiptingResponse();

        JourneyContext context = new JourneyContext(
                correlationId,
                receiptingRequest.getPartnerCode(),
                receiptingRequest.getParams()
        );

        // Fire async — HTTP thread returns here immediately with correlationId
        // processJourney() picks up on journeyTaskExecutor thread pool
        receipting(context, journeyStart);

        receiptingResponse.setReceiptNo(context.getReceiptingResult().getReceiptNo());
        return receiptingResponse;
    }

    public void receipting(JourneyContext context, long journeyStart) {
        Set<String> succeeded = trackingService.getSucceededApiNames(context.getCorrelationId());
        log.info("[{}] Receipting Journey execute | alreadySucceeded={}",
                context.getCorrelationId(), succeeded);

        log.info("Start time: {}", LocalTime.now());
        log.info(":::::::::::::: Method receipting() Starts :::::::::::::::::");

        Map<String, String> params = context.getRawParams();
        ReceiptingResponse receiptingResponse = new ReceiptingResponse();

        ReceiptingRequest receiptingRequest = new ReceiptingRequest();
        receiptingRequest.setAccountCode(null);
        receiptingRequest.setAccountType(null);
        receiptingRequest.setAgentCode(params.get("pnlObj1.stringval4"));
        receiptingRequest.setAuthCode(null);
        receiptingRequest.setBankBranchCode(null);
        receiptingRequest.setCardNo(null);
        receiptingRequest.setCardType(params.get("pnlObj5.stringval3"));
        receiptingRequest.setCtsNonCts(null);
        receiptingRequest.setCustomerAccountNo(null);
        receiptingRequest.setCustomerBankIfscCode(null);
        receiptingRequest.setDepositBankCode(null);
        receiptingRequest.setDepositBankName(params.get("pnlObj5.stringval23"));
        receiptingRequest.setDraweeBankName(params.get("pnlObj5.stringval14"));
        receiptingRequest.setEmailID(params.get("pnlObj1.stringval29"));
        receiptingRequest.setGatewayMessage(null);
        String holderName = buildFullName(params.get("pnlObj1.stringval22"), params.get("pnlObj1.stringval23"), params.get("pnlObj1.stringval24"));
        receiptingRequest.setHolderName(holderName);
        receiptingRequest.setIfscCode(null);
        receiptingRequest.setInstrumentNO(null);
        receiptingRequest.setIssuingBankName(null);
        receiptingRequest.setMerchantNo(null);
        receiptingRequest.setMicrCode(null);
        receiptingRequest.setMobileNo(params.get("pnlObj1.stringval28"));
        receiptingRequest.setPaymentSource(params.get("pnlObj1.stringval1") + "_NB");
        receiptingRequest.setRenterInstrumentNO(null);
        receiptingRequest.setUtrNO(null);
        receiptingRequest.setOnlinePayMode(params.get("pnlObj5.stringval13"));
        receiptingRequest.setPremiumType("INITIAL_PREMIUM");
        receiptingRequest.setApplicationNo(params.get("pnlObj1.stringval6"));
        receiptingRequest.setPolicyNo(null);
        receiptingRequest.setTransactionAmount(params.get("pnlObj5.stringval1"));
        receiptingRequest.setBalicTransactionId(params.get("pnlObj5.stringval11"));
        receiptingRequest.setPaymentChannel(params.get("pnlObj5.stringval10"));
        receiptingRequest.setProcessedBy(null);
        receiptingRequest.setTpslId(params.get("pnlObj5.stringval12"));
        receiptingRequest.setPaymentGatewayID(null);
        receiptingRequest.setBankTransactionId(params.get("pnlObj5.stringval12"));
        receiptingRequest.setTopUpProposal(null);
        receiptingRequest.setTopUpAmount(null);
        receiptingRequest.setUniqueId(null);
        receiptingRequest.setNbPayMode(null);
        receiptingRequest.setNbId(null);

        // Call RECEIPTING API
        context.setReceiptingResult(receiptingApiClient.call(receiptingRequest, context));

        // Log the receipting outcome. (Previously logged context.getCibilResult()[0] here,
        // but the receipting flow never calls CIBIL — that was a guaranteed NPE.)
        var receiptingResult = context.getReceiptingResult();
        log.info("[{}] RECEIPTING complete | receiptNo={}",
                context.getCorrelationId(),
                receiptingResult != null ? receiptingResult.getReceiptNo() : null);
    }

    public static String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder nameBuilder = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty() && !"null".equalsIgnoreCase(firstName)) {
            nameBuilder.append(firstName.trim());
        }
        if (middleName != null && !middleName.trim().isEmpty() && !"null".equalsIgnoreCase(middleName)) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(middleName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty() && !"null".equalsIgnoreCase(lastName)) {
            if (nameBuilder.length() > 0) nameBuilder.append(" ");
            nameBuilder.append(lastName.trim());
        }
        return nameBuilder.toString();
    }
}
