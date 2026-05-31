package com.balic.newbusiness.integration.client;

import com.balic.newbusiness.exception.ApiCallException;
import com.balic.newbusiness.exception.JourneyStageException;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.tracking.JourneyTrackingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NGINTokenAPIClient {

    private static final Logger log       = LoggerFactory.getLogger(NGINTokenAPIClient.class);
    private static final String STAGE     = "NGIN_STAGE";
    private static final String NGIN_NAME  = "NGIN_API";

    @Value("${api.endpoints.generateNginToken}")
    private String generateNginTokenUrl;

    @Autowired private RestTemplate restTemplate;
    @Autowired private JourneyTrackingService trackingService;
    @Autowired private ObjectMapper objectMapper;

    @Retryable(
            value  = {ApiCallException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )

    public String GenerateNginTokenOSB() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = "{\"serverEnv\":\"uat2\"}";

        log.info("GenerateNginTokenOSB_API request : {}", jsonBody);

        HttpEntity<String> generateNginTokenRequest = new HttpEntity<>(jsonBody, headers);
        String token = null;

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(generateNginTokenUrl, generateNginTokenRequest, Map.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {

                log.info("GenerateNginTokenOSB_API response : {}",  String.valueOf(objectMapper.writeValueAsString(responseEntity.getBody())));

                token = (String) responseEntity.getBody().get("token");

                System.out.println("Extracted Token: " + token);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    @Recover
    public String recover(ApiCallException ex, JourneyContext context) {

        log.error("[{}] {} — all retries exhausted: {}", context.getCorrelationId(), NGIN_NAME, ex.getMessage());

        // JourneyStageException stops the journey.
        // JourneyOrchestrator catches this, marks journey as FAILED.
        // On next retry run, this API will re-execute (not in SUCCESS set).
        throw new JourneyStageException(STAGE,
                NGIN_NAME + " failed after all retries: " + ex.getMessage());
    }
}
