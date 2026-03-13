package com.insurance.newbusiness.reversefeed;

import com.insurance.newbusiness.domain.entity.PartnerConfig;
import com.insurance.newbusiness.exception.PartnerConfigException;
import com.insurance.newbusiness.journey.JourneyContext;
import com.insurance.newbusiness.repository.PartnerConfigRepository;
import com.insurance.newbusiness.tracking.JourneyTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
// PartnerNotifier interface
// ─────────────────────────────────────────────────────────────────────────────
interface PartnerNotifier {
    String getSupportedPartnerCode();
    void notify(JourneyContext context, String applicationNumber);
}

// ─────────────────────────────────────────────────────────────────────────────
// PartnerNotifierFactory
//
// Spring auto-collects all PartnerNotifier beans into the List on construction.
// Adding a new partner = add a new @Component. Nothing else changes here.
// ─────────────────────────────────────────────────────────────────────────────
@Service
public class PartnerNotifierFactory {

    private final Map<String, PartnerNotifier> notifiers = new HashMap<>();

    public PartnerNotifierFactory(List<PartnerNotifier> notifierList) {
        for (PartnerNotifier notifier : notifierList) {
            notifiers.put(notifier.getSupportedPartnerCode(), notifier);
        }
    }

    public PartnerNotifier getNotifier(String partnerCode) {
        PartnerNotifier notifier = notifiers.get(partnerCode);
        if (notifier == null) {
            throw new PartnerConfigException(
                    "No PartnerNotifier for partnerCode: " + partnerCode +
                    ". Add a @Component implementing PartnerNotifier.");
        }
        return notifier;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DefaultPartnerNotifier — handles the current single partner.
//
// Reads reverse_feed_url and auth from partner_config table.
// Retries up to 3 times if partner endpoint is temporarily unavailable.
//
// To add a new partner: copy this class, change PARTNER_CODE and notify() logic.
// ─────────────────────────────────────────────────────────────────────────────
@Component
class DefaultPartnerNotifier implements PartnerNotifier {

    private static final Logger log = LoggerFactory.getLogger(DefaultPartnerNotifier.class);
    private static final String PARTNER_CODE = "PARTNER_A"; // TODO: set real partner code

    @Autowired private RestTemplate restTemplate;
    @Autowired private PartnerConfigRepository partnerConfigRepository;
    @Autowired private JourneyTrackingService trackingService;

    @Override
    public String getSupportedPartnerCode() { return PARTNER_CODE; }

    @Override
    @Retryable(value = {RuntimeException.class}, maxAttempts = 3,
               backoff = @Backoff(delay = 5000, multiplier = 2))
    public void notify(JourneyContext context, String applicationNumber) {
        long start = System.currentTimeMillis();

        PartnerConfig config = partnerConfigRepository.findById(context.getPartnerCode())
                .orElseThrow(() -> new PartnerConfigException(
                        "No partner_config for: " + context.getPartnerCode()));

        log.info("[{}] Sending reverse feed | partner={} | url={}",
                context.getCorrelationId(), context.getPartnerCode(), config.getReverseFeedUrl());

        try {
            HttpHeaders headers = buildAuthHeaders(config);
            Map<String, String> payload = new HashMap<>();
            payload.put("correlationId",    context.getCorrelationId());
            payload.put("applicationNumber", applicationNumber);
            // TODO: add other fields required by partner's reverse feed contract

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.exchange(config.getReverseFeedUrl(), HttpMethod.POST, request, Void.class);

            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, "REVERSE_FEED_STAGE", "REVERSE_FEED_API",
                    null, null, "SUCCESS", null, null, duration);
            log.info("[{}] Reverse feed sent | {}ms", context.getCorrelationId(), duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            trackingService.logApiCall(context, "REVERSE_FEED_STAGE", "REVERSE_FEED_API",
                    null, null, "FAILED", "REVERSE_FEED_ERROR", ex.getMessage(), duration);
            log.error("[{}] Reverse feed failed: {}", context.getCorrelationId(), ex.getMessage());
            throw ex;
        }
    }

    private HttpHeaders buildAuthHeaders(PartnerConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if ("BEARER".equalsIgnoreCase(config.getAuthType())) {
            headers.setBearerAuth(config.getAuthCredential());
        } else if ("API_KEY".equalsIgnoreCase(config.getAuthType())) {
            headers.set("X-Api-Key", config.getAuthCredential());
        }
        // TODO: add BASIC auth if required
        return headers;
    }
}
