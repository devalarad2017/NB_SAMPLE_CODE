package com.balic.newbusiness.integration.model.pas;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;


/**
 * PasRequest — sent to PAS (Policy Administration System) to create the application.
 *
 * This is the richest request in the journey because it aggregates data from:
 *   - Partner's raw inbound params (stringvalN via property-file mapping)
 *   - All prior API responses (set manually in PasApiClient.buildPasRequest)
 *
 * FIELD SOURCES:
 *   [RAW]   = from partner's stringvalN, mapped via mapping/default-PAS_API.properties
 *   [RESP]  = from prior API response, set manually in PasApiClient.buildPasRequest()
 *   [CTX]   = from JourneyContext (correlationId, partnerCode)
 *
 * Property file maps RAW fields. RESP fields are set directly in code because
 * they come from typed response objects, not from partner's raw params.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasRequest {


    private PasHeader header;
    private PasRequestBody request;

}
