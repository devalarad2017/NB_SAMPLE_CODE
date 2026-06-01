package com.balic.newbusiness.tracking;

import com.balic.newbusiness.domain.entity.JourneyStageLog;
import com.balic.newbusiness.integration.model.auw.AuwResponse;
import com.balic.newbusiness.integration.model.bi.BiResponse;
import com.balic.newbusiness.integration.model.cibil.CibilResponse;
import com.balic.newbusiness.integration.model.dcs.DcsResponse;
import com.balic.newbusiness.integration.model.edc.EdcResponse;
import com.balic.newbusiness.integration.model.ekyc.EkycResponse;
import com.balic.newbusiness.integration.model.fes.FesResponse;
import com.balic.newbusiness.integration.model.mrs.MrsResponse;
import com.balic.newbusiness.integration.model.pan.PanResponse;
import com.balic.newbusiness.integration.model.pennydrop.PennyDropResponse;
import com.balic.newbusiness.integration.model.proposal.ProposalResponse;
import com.balic.newbusiness.integration.model.ucs.UcsResponse;
import com.balic.newbusiness.journey.JourneyContext;
import com.balic.newbusiness.repository.JourneyStageLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * JourneyResultRestorer — on a RESUMED journey, reloads the results of APIs that
 * already succeeded, so the in-memory context matches what a fresh run would hold.
 *
 * ── WHY THIS EXISTS ──────────────────────────────────────────────────────────
 * When a FAILED journey is resumed, JourneyOrchestrator SKIPS any API already marked
 * SUCCESS (so we never re-call CIBIL, re-push PAS, etc.). But skipping means that
 * API's typed result is never rebuilt in memory — and later stages (EDC, PAS, …) read
 * those results. So this class reloads each succeeded API's last stored response from
 * journey_stage_log and deserialises it back onto JourneyContext. A resumed run then
 * has exactly the data a fresh run would have built in memory.
 *
 * Failures are logged and skipped — they never abort the journey; a missing restored
 * value simply behaves as it would on a fresh run.
 */
@Component
public class JourneyResultRestorer {

    private static final Logger log = LoggerFactory.getLogger(JourneyResultRestorer.class);

    private final JourneyStageLogRepository stageLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * One handler per API: knows how to turn a stored JSON payload back into the
     * typed result and set it on the context. Keys match the api_name values used
     * by the ApiClients and the skip checks in JourneyOrchestrator.
     */
    private final Map<String, BiConsumer<JourneyContext, String>> handlers = new HashMap<>();

    public JourneyResultRestorer(JourneyStageLogRepository stageLogRepository, ObjectMapper objectMapper) {
        this.stageLogRepository = stageLogRepository;
        this.objectMapper = objectMapper;
        handlers.put("CIBIL_API", (ctx, json) -> ctx.setCibilResult(read(json, CibilResponse[].class)));
        handlers.put("UCS_API",   (ctx, json) -> ctx.setUcsResult(read(json, UcsResponse.class)));
        handlers.put("PAN_API",   (ctx, json) -> ctx.setPanResult(read(json, PanResponse.class)));
        handlers.put("BI_API",    (ctx, json) -> ctx.setBiResult(read(json, BiResponse.class)));
        handlers.put("EKYC_API",  (ctx, json) -> ctx.setEkycResult(read(json, EkycResponse.class)));
        handlers.put("EDC_API",   (ctx, json) -> ctx.setEdcResult(read(json, EdcResponse.class)));
        handlers.put("DCS_API",   (ctx, json) -> ctx.setDcsResult(read(json, DcsResponse.class)));
        handlers.put("AUW_API",   (ctx, json) -> ctx.setAuwResult(read(json, AuwResponse.class)));
        handlers.put("MRS_API",   (ctx, json) -> ctx.setMrsResult(read(json, MrsResponse.class)));
        handlers.put("FES_API",   (ctx, json) -> ctx.setFesResult(read(json, FesResponse.class)));
        handlers.put("PROPOSAL_API", (ctx, json) -> ctx.setProposalResult(read(json, ProposalResponse.class)));
        handlers.put("PENNYDROP_API", (ctx, json) -> ctx.setPennyDropResult(read(json, PennyDropResponse.class)));
    }

    /**
     * Repopulates JourneyContext from the latest SUCCESS log row per API.
     * No-op on a first run (no SUCCESS rows yet).
     */
    public void restorePriorResults(JourneyContext context) {
        List<JourneyStageLog> successLogs;
        try {
            successLogs = stageLogRepository.findSuccessLogs(context.getCorrelationId());
        } catch (Exception ex) {
            log.error("[{}] Could not load success logs to restore — continuing as fresh run: {}",
                    context.getCorrelationId(), ex.getMessage());
            return;
        }

        if (successLogs == null || successLogs.isEmpty()) {
            return; // fresh run — nothing to restore
        }

        int restored = 0;
        // Rows are ordered by id ASC, so the last write for an api_name wins.
        for (JourneyStageLog logRow : successLogs) {
            BiConsumer<JourneyContext, String> handler = handlers.get(logRow.getApiName());
            if (handler == null || logRow.getResponsePayload() == null) {
                continue;
            }
            try {
                handler.accept(context, logRow.getResponsePayload());
                restored++;
            } catch (Exception ex) {
                log.warn("[{}] Could not restore {} from log — skipping: {}",
                        context.getCorrelationId(), logRow.getApiName(), ex.getMessage());
            }
        }
        log.info("[{}] Restored {} prior API result(s) for resume", context.getCorrelationId(), restored);
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("Could not deserialize stored payload into {}: {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }
}
