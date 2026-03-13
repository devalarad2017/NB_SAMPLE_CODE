package com.insurance.newbusiness.domain.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks overall status of one request's journey.
 *
 * NOTE: last_completed_stage has been REMOVED.
 * Resume logic now uses journey_stage_log (api_name + status=SUCCESS).
 * This gives API-level resume granularity, not just stage-level.
 */
@Data
@Entity
@Table(name = "journey_execution")
public class JourneyExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", unique = true, nullable = false, length = 36)
    private String correlationId;

    @Column(name = "partner_code", length = 20)
    private String partnerCode;

    @Column(name = "raw_request_id")
    private Long rawRequestId;

    // IN_PROGRESS | COMPLETED | FAILED
    @Column(name = "overall_status", length = 20)
    private String overallStatus;

    // Set after PAS returns success — passed to reverse feed
    @Column(name = "application_number", length = 100)
    private String applicationNumber;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
