package com.insurance.newbusiness.integration.model.kyc;
import lombok.Data;
/** KycResponse — received from KYC_API. TODO: Replace with actual contract fields. */
@Data
public class KycResponse {
    private String status;      // VERIFIED / FAILED / PENDING
    private String kycRefId;
    private String remarks;
}
