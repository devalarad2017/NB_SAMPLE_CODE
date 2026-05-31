package com.balic.newbusiness.integration.client;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NGINTokenAPIClient {

    private static final Logger log       = LoggerFactory.getLogger(NGINTokenAPIClient.class);
    private static final String STAGE     = "NGIN_STAGE";
    private static final String NGIN_NAME  = "NGIN_API";

    @Value("${api.endpoints.generateNginToken}")
    private final String generateNginTokenUrl;

    private final RestClient            restClient;
    private final JourneyTrackingService trackingService;
    private final ObjectMapper          objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String GenerateNginTokenOSB() {
        String jsonBody = "{\"serverEnv\":\"uat2\"}";
        log.info("GenerateNginTokenOSB_API request : {}", jsonBody);

        String token = null;
        try {
            long start = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(generateNginTokenUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(Map.class);
            long duration = System.currentTimeMillis() - start;

            if (response != null) {
                log.info("GenerateNginTokenOSB_API response : {}",
                        String.valueOf(objectMapper.writeValueAsString(response)));
                token = (String) response.get("token");
            }
            // Surface a missing token explicitly instead of returning null silently.
            if (token == null || token.isBlank()) {
                log.error("NGIN token generation returned no token | {}ms", duration);
            } else {
                log.info("NGIN token generated successfully | {}ms", duration);
            }
        } catch (Exception e) {
            log.error("NGIN token generation failed: {}", e.getMessage(), e);
        }
        return token;
    }

    @Recover
    public String recover(ApiCallException ex, JourneyContext context) {
        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), NGIN_NAME, ex.getMessage());
        throw new JourneyStageException(STAGE,
                NGIN_NAME + " failed after all retries: " + ex.getMessage());
    }
}
