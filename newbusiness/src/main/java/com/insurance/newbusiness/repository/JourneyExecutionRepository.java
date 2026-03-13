package com.insurance.newbusiness.repository;

import com.insurance.newbusiness.domain.entity.JourneyExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JourneyExecutionRepository extends JpaRepository<JourneyExecution, Long> {
    Optional<JourneyExecution> findByCorrelationId(String correlationId);
}
