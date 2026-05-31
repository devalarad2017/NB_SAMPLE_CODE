package com.balic.newbusiness.domain.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "partner_config")
public class PartnerConfig {

    @Id
    @Column(name = "partner_code")
    private String partnerCode;

    @Column(name = "reverse_feed_url")
    private String reverseFeedUrl;

    @Column(name = "auth_type")
    private String authType;        // BEARER | BASIC | API_KEY .use 3-scale token generation

    @Column(name = "auth_credential")
    private String authCredential;  // store encrypted //Temp .use 3-scale token generation

    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;

    @Column(name = "is_active")
    private boolean active = true;
}
