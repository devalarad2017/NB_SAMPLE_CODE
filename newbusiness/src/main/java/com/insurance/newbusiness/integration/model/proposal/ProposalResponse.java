package com.insurance.newbusiness.integration.model.proposal;
import lombok.Data;
/** ProposalResponse — received from PROPOSAL_SUBMIT_API. TODO: Replace with actual fields. */
@Data
public class ProposalResponse {
    private String status;
    private String proposalNumber;  // used as input to PAS submission
    private String remarks;
}
