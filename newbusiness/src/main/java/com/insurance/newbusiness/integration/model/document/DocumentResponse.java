package com.insurance.newbusiness.integration.model.document;
import lombok.Data;
/** DocumentResponse — received from DOCUMENT_API. TODO: Replace with actual fields. */
@Data
public class DocumentResponse {
    private String status;
    private String documentId;
    private String documentUrl;  // pre-signed URL or document reference
}
