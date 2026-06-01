package com.balic.newbusiness.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.balic.newbusiness.domain.entity.JourneyStageLog;

import java.util.List;
import java.util.Set;

public interface JourneyStageLogRepository extends JpaRepository<JourneyStageLog, Long> {

    /**
     * Returns the set of api_names that have already succeeded for this correlationId.
     * Used by JourneyOrchestrator to skip already-completed API steps on resume.
     */
    @Query("SELECT l.apiName FROM JourneyStageLog l " +
           "WHERE l.correlationId = :correlationId AND l.status = 'SUCCESS'")
    Set<String> findSucceededApiNames(@Param("correlationId") String correlationId);

    /**
     * Returns all SUCCESS rows for a correlationId in insertion order.
     * JourneyResultRestorer iterates these and keeps the latest per api_name to
     * rebuild JourneyContext on resume.
     */
    @Query("SELECT l FROM JourneyStageLog l " +
           "WHERE l.correlationId = :correlationId AND l.status = 'SUCCESS' " +
           "ORDER BY l.id ASC")
    List<JourneyStageLog> findSuccessLogs(@Param("correlationId") String correlationId);

    /**
     * Returns the most recent FAILED row for a correlationId — i.e. the API/stage
     * where the journey last stopped. Used to populate journey_execution failure fields.
     */
    JourneyStageLog findTopByCorrelationIdAndStatusOrderByIdDesc(String correlationId, String status);

    /**
     * Returns the full stage-log history for a correlationId (newest first) for UI display.
     */
    List<JourneyStageLog> findByCorrelationIdOrderByIdDesc(String correlationId);

    /**
     * Full stage-log history for an application number (newest first) — the UI's
     * primary search, since application_number is the business tracking key.
     */
    List<JourneyStageLog> findByApplicationNumberOrderByIdDesc(String applicationNumber);

    /**
     * Most recent log row for an application number — used to resolve the
     * correlationId backing that application number for status/resume.
     */
    JourneyStageLog findFirstByApplicationNumberOrderByIdDesc(String applicationNumber);
}
