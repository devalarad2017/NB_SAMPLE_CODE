package com.balic.newbusiness.journey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.balic.newbusiness.integration.model.auw.AuwResponse;
import com.balic.newbusiness.integration.model.bi.BiResponse;
import com.balic.newbusiness.integration.model.cibil.CibilResponse;
import com.balic.newbusiness.integration.model.dcs.DcsResponse;
import com.balic.newbusiness.integration.model.edc.EdcResponse;
import com.balic.newbusiness.integration.model.ekyc.EkycResponse;
import com.balic.newbusiness.integration.model.fes.FesResponse;
import com.balic.newbusiness.integration.model.mrs.MrsResponse;
import com.balic.newbusiness.integration.model.pan.PanResponse;
import com.balic.newbusiness.integration.model.pennydrop.PennyDropResponse;
import com.balic.newbusiness.integration.model.proposal.ProposalResponse;
import com.balic.newbusiness.integration.model.receipting.ReceiptingResponse;
import com.balic.newbusiness.integration.model.ucs.UcsResponse;
import lombok.Data;

/**
 * JourneyContext — shared state for one request's end-to-end journey.
 *
 * Created once in NewBusinessService.receiveAndAcknowledge() and passed
 * through every stage in JourneyOrchestrator.
 *
 * ── TYPED RESULTS vs MAP ─────────────────────────────────────────────────────
 * Each stage sets its typed result on this context after the API call.
 * Later stages read from these typed results directly — no Map casting needed.

 * ── ADDING A NEW API RESULT ───────────────────────────────────────────────────
 * 1. Add private XxxResponse xxxResult field
 * 2. Add getter and setter
 * 3. Set it in JourneyOrchestrator after the API call
 * 4. Read it in later stages when building downstream requests
 */
public class JourneyContext {

    // ──  core params ───────────────────────────────────────────────
    private final String correlationId;   // unique in every log line via MDC
    private final String partnerCode;     // used for mapping lookup and reverse feed
    private final Map<String, String> rawParams; // merged from pInObj1+2+3 — all stringvalN in one map

    // ── Journey result — set after PAS returns ────────────────────────────────
    private String applicationNumber;

    // ── Typed stage results — set by JourneyOrchestrator after each API call ──
    private CibilResponse[]        cibilResult;       // first stage — credit check
    private UcsResponse			 ucsResult;
    private PanResponse			 panResult;
    private BiResponse			 biResult;
	private EdcResponse          edcResult;
    private DcsResponse          dcsResult;
    private AuwResponse          auwResult;
    private MrsResponse          mrsResult;
    private FesResponse          fesResult;
    private EkycResponse 		 ekycResult;
    private PennyDropResponse    pennyDropResult;
    private ReceiptingResponse   receiptingResult;
    private ProposalResponse     proposalResult;


    public JourneyContext(String correlationId, String partnerCode, Map<String, String> rawParams) {
        this.correlationId = correlationId;
        this.partnerCode   = partnerCode;
        this.rawParams     = Collections.unmodifiableMap(new HashMap<>(rawParams));
    }

    // ── Core params getters ─────────────────────────────────────────────────
    public String getCorrelationId() {
    	return correlationId;
    }
    public String getPartnerCode() { 
    	return partnerCode; 
    }
    public Map<String, String> getRawParams(){ 
    	return rawParams; 
    }
    public String getApplicationNumber(){ 
    	return applicationNumber; 
    }
    public void setApplicationNumber(String appNo){
    	this.applicationNumber =appNo;
    }

    // ── Stage result getters/setters ─────────────────────────────────────────

    public CibilResponse[] getCibilResult() { 
    	return cibilResult; 
    }
    public void setCibilResult(CibilResponse[] cibilResponses){ 
    	this.cibilResult = cibilResponses; 	
    }
    
    public UcsResponse getUcsResult(){ 
    	return ucsResult; 
    }
    public void setUcsResult(UcsResponse ucs){
    	this.ucsResult = ucs; 
    }
    public EdcResponse getEdcResult() {
		return edcResult;
	}

	public void setEdcResult(EdcResponse edcResult) {
		this.edcResult = edcResult;
	}

	public DcsResponse getDcsResult() {
		return dcsResult;
	}

	public void setDcsResult(DcsResponse dcsResult) {
		this.dcsResult = dcsResult;
	}

	public AuwResponse getAuwResult() {
		return auwResult;
	}

	public void setAuwResult(AuwResponse auwResult) {
		this.auwResult = auwResult;
	}

	public MrsResponse getMrsResult() {
		return mrsResult;
	}

	public void setMrsResult(MrsResponse mrsResult) {
		this.mrsResult = mrsResult;
	}

	public FesResponse getFesResult() {
		return fesResult;
	}

	public void setFesResult(FesResponse fesResult) {
		this.fesResult = fesResult;
	}

	public PanResponse getPanResult() {
		return panResult;
	}

	public void setPanResult(PanResponse panResult) {
		this.panResult = panResult;
	}

	public BiResponse getBiResult() {
		return biResult;
	}

	public void setBiResult(BiResponse biResult) {
		this.biResult = biResult;
	}

	public EkycResponse getEkycResult() {
		return ekycResult;
	}

	public void setEkycResult(EkycResponse ekycResult) {
		this.ekycResult = ekycResult;
	}

    public ReceiptingResponse getReceiptingResult() {
        return receiptingResult;
    }

    public void setReceiptingResult(ReceiptingResponse receiptingResult) {
        this.receiptingResult = receiptingResult;
    }

    public ProposalResponse getProposalResult() {
        return proposalResult;
    }

    public void setProposalResult(ProposalResponse proposalResult) {
        this.proposalResult = proposalResult;
    }

    public PennyDropResponse getPennyDropResult() {
        return pennyDropResult;
    }

    public void setPennyDropResult(PennyDropResponse pennyDropResult) {
        this.pennyDropResult = pennyDropResult;
    }



}
