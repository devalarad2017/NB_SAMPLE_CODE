package com.insurance.newbusiness.controller;

import com.insurance.newbusiness.domain.dto.ErrorResponse;
import com.insurance.newbusiness.domain.dto.InboundRequest;
import com.insurance.newbusiness.domain.dto.NotificationResponse;
import com.insurance.newbusiness.service.NewBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Single inbound endpoint for receiving insurance applications from external partners.
 *
 * Flow:
 *   1. Validate request
 *   2. Store raw payload to DB
 *   3. Return 202 Accepted + correlationId immediately
 *   4. Process journey asynchronously on background thread
 *   5. Partner receives application number via reverse feed callback when done
 *
 * Swagger UI: http://localhost:8080/swagger-ui.html
 */
@Tag(name = "New Business", description = "Insurance new business application APIs")
@RestController
@RequestMapping("/api/v1")
public class NewBusinessController {

    @Autowired
    private NewBusinessService newBusinessService;

    @Operation(
        summary = "Submit insurance application",
        description = "Receives 600+ generic params from external partner. " +
                      "Stores raw request immediately and returns 202 with correlationId. " +
                      "Full journey runs asynchronously. " +
                      "Partner receives application number via registered reverse feed URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Request accepted and processing started",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing mandatory mapping fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/newbusiness")
    public ResponseEntity<NotificationResponse> receiveRequest(
            @RequestBody InboundRequest inboundRequest) {

        // TODO: add header-based API key / token validation here before processing

        String correlationId = newBusinessService.receiveAndAcknowledge(inboundRequest);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new NotificationResponse(
                        correlationId,
                        "Request received. Application number will be sent via reverse feed."));
    }
}
