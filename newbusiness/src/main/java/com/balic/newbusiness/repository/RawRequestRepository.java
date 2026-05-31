package com.balic.newbusiness.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.balic.newbusiness.domain.entity.RawRequest;

import java.util.Optional;

public interface RawRequestRepository extends JpaRepository<RawRequest, Long> {

    /**
     * Loads the immutable raw payload for a correlationId so a journey can be
     * resumed from UI with exactly the same data the partner originally sent.
     */
    Optional<RawRequest> findByCorrelationId(String correlationId);
}
