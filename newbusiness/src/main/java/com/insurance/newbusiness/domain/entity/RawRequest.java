package com.insurance.newbusiness.domain.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "raw_request")
public class RawRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "correlation_id", unique = true, nullable = false, length = 36)
    private String correlationId;
    @Column(name = "partner_code", nullable = false, length = 20)
    private String partnerCode;
    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;
    @Column(name = "received_at", updatable = false)
    private LocalDateTime receivedAt;
    @PrePersist public void prePersist() { receivedAt = LocalDateTime.now(); }
}
