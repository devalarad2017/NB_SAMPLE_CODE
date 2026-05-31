package com.balic.newbusiness.domain.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "raw_request")
public class RawRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "correlation_id")
    private String correlationId;
    @Column(name = "partner_code")
    private String partnerCode;
    @Column(name = "raw_payload")
    private String rawPayload;
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    @PrePersist public void prePersist() {
    	receivedAt = LocalDateTime.now(); 
    }
}
