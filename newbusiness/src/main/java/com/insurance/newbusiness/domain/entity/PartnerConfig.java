package com.insurance.newbusiness.domain.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "partner_config")
public class PartnerConfig {

    @Id
    @Column(name = "partner_code", length = 20)
    private String partnerCode;

    @Column(name = "reverse_feed_url", nullable = false, length = 500)
    private String reverseFeedUrl;

    @Column(name = "auth_type", length = 20)
    private String authType;        // BEARER | BASIC | API_KEY

    @Column(name = "auth_credential")
    private String authCredential;  // store encrypted

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;

    @Column(name = "is_active")
    private boolean active = true;
}
