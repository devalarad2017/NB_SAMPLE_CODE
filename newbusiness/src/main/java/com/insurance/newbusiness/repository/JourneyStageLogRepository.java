package com.insurance.newbusiness.repository;

import com.insurance.newbusiness.domain.entity.JourneyStageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;

public interface JourneyStageLogRepository extends JpaRepository<JourneyStageLog, Long> {

    /**
     * Returns the set of api_names that have already succeeded for this correlationId.
     * Used by JourneyOrchestrator to skip already-completed API steps on retry.
     */
    @Query("SELECT l.apiName FROM JourneyStageLog l " +
           "WHERE l.correlationId = :correlationId AND l.status = 'SUCCESS'")
    Set<String> findSucceededApiNames(@Param("correlationId") String correlationId);
}
