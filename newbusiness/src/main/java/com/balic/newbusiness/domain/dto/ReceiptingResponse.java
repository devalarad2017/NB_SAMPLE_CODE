package com.balic.newbusiness.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ReceiptingResponse — acknowledgement returned from the /receipting endpoint.
 *
 * NOTE: this file previously contained a duplicate copy of NotificationResponse
 * (wrong class for the filename), which broke compilation of the whole module
 * (a fatal javac error makes Lombok abort code-generation, cascading "cannot find
 * symbol" errors everywhere). Restored to the ReceiptingResponse used by
 * ReceiptingService / NewBusinessController.
 */
@Data
@NoArgsConstructor
@Schema(description = "Receipting acknowledgement returned to the caller")
public class ReceiptingResponse {

    @Schema(description = "Receipt number returned by the receipting system")
    private String receiptNo;
}
