package com.balic.newbusiness.controller;

import com.balic.newbusiness.domain.dto.*;
import com.balic.newbusiness.integration.model.bi.BiResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.journey.JourneyOrchestrator;
import com.balic.newbusiness.service.ReceiptingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.balic.newbusiness.service.NewBusinessService;

/**
 * Single inbound endpoint for receiving insurance applications from external partners.
 *
 * Steps:
 *   1. Validate request
 *   2. Store request payload to DB
 *   3. Return 202 Accepted + Appno immediately
 *   4. Process journey asynchronously on background
 *   5. Partner receives application number via reverse feed api when PAS app pushed 
 *
 */
@Tag(name = "New Business", description = "Bajajlife new business application APIs")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NewBusinessController {

    private final NewBusinessService newBusinessService;
    private final ReceiptingService receiptingService;

    //Temporary to check BI
    private final JourneyOrchestrator journeyOrchestrator;

    @Operation(
        summary = "Submit NB application",
        description = "Receives partner payload with pInObj1-3_inout (key-value maps) " +
                      "and pInList1-6_inout (list records) containing stringvalN params. " +
                      "Store request immediately and returns 202 with Appno. " +
                      "Full journey runs asynchronously. " +
                      "Partner receives application number via registered reverse feed URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Request accepted and processing started",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing mandatory fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/newbusiness")
    public ResponseEntity<NotificationResponse> receiveRequest(
            @RequestBody InboundRequest inboundRequest) {

        String correlationId = newBusinessService.receiveAndAcknowledge(inboundRequest); //temp this id. Use app no later 

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new NotificationResponse(

                        correlationId,
                        "Request received. Application number will be sent via reverse feed."));
    }

    @Operation(
        summary = "Retry / resume a failed journey",
        description = "Re-runs a previously FAILED journey for the given correlationId. " +
                      "Already-succeeded APIs are skipped and their results restored, so " +
                      "processing resumes from the stage that failed, using the original data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Retry accepted and resuming",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Unknown correlationId",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/newbusiness/{correlationId}/retry")
    public ResponseEntity<NotificationResponse> retryJourney(@PathVariable String correlationId) {
        newBusinessService.retryJourney(correlationId);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new NotificationResponse(
                        correlationId,
                        "Retry accepted. Journey is resuming from the last failed stage."));
    }

    @Operation(
        summary = "Get journey status",
        description = "Returns the overall status, the stage/API where the journey stopped " +
                      "(if any), the application number once available, and the full stage history."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Journey status",
            content = @Content(schema = @Schema(implementation = JourneyStatusResponse.class))),
        @ApiResponse(responseCode = "400", description = "Unknown correlationId",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/newbusiness/{correlationId}/status")
    public ResponseEntity<JourneyStatusResponse> getJourneyStatus(@PathVariable String correlationId) {
        return ResponseEntity.ok(newBusinessService.getStatus(correlationId));
    }

    @Operation(
        summary = "Get journey status by application number",
        description = "Business-key lookup: returns the same status view searched by the " +
                      "partner-supplied application number instead of the technical correlationId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Journey status",
            content = @Content(schema = @Schema(implementation = JourneyStatusResponse.class))),
        @ApiResponse(responseCode = "400", description = "Unknown application number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/newbusiness/application/{applicationNumber}/status")
    public ResponseEntity<JourneyStatusResponse> getStatusByApplicationNumber(
            @PathVariable String applicationNumber) {
        return ResponseEntity.ok(newBusinessService.getStatusByApplicationNumber(applicationNumber));
    }

    @Operation(
        summary = "Retry / resume a failed journey by application number",
        description = "Business-key retry: resumes the failed journey for the given " +
                      "application number from the stage that failed, using the original data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Retry accepted and resuming",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Unknown application number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/newbusiness/application/{applicationNumber}/retry")
    public ResponseEntity<NotificationResponse> retryByApplicationNumber(
            @PathVariable String applicationNumber) {
        String correlationId = newBusinessService.retryByApplicationNumber(applicationNumber);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new NotificationResponse(
                        correlationId,
                        "Retry accepted for application " + applicationNumber
                                + ". Journey is resuming from the last failed stage."));
    }

    @PostMapping(path = "/receipting", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceiptingResponse> getPremiumDetails(@RequestBody InboundReceiptingRequest request) {
        ReceiptingResponse receiptingResponse = receiptingService.receiveAndAcknowledge(request);
        return new ResponseEntity<>(receiptingResponse, HttpStatus.OK);
    }

    //Temporary to check BI
    @PostMapping("/bi/execute")
    public BiResponse executeBiStageAndGetResponse(@RequestBody InboundRequest inboundRequest) {
        // You may want to validate and build params as needed
        String correlationId = "manual-corr-id";
        JourneyContext context = new JourneyContext(
                correlationId,
                inboundRequest.getPartnerCode(),
                inboundRequest.getParams()
        );
        // Call only BI stage
        journeyOrchestrator.executeBi(context);
        // Return the BI response
        return context.getBiResult();
    }
}