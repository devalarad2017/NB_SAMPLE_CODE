package com.balic.newbusiness.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.balic.newbusiness.domain.entity.JourneyExecution;

import java.util.Optional;

public interface JourneyExecutionRepository extends JpaRepository<JourneyExecution, Long> {
    Optional<JourneyExecution> findByCorrelationId(String correlationId);
}
