package com.insurance.newbusiness.integration.model.document;
import lombok.Data;
/** DocumentRequest — sent to DOCUMENT_API for policy document generation. TODO: Fill fields. */
@Data
public class DocumentRequest {
    private String correlationId;
    private String productCode;
    private String applicantName;
    // TODO: Add all fields from DOCUMENT_API contract
}
