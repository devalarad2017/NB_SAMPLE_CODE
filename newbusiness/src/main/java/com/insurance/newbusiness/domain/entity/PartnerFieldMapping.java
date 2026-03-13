package com.insurance.newbusiness.domain.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * One row = one field mapping from a generic inbound param (stringvalN)
 * to a specific field in a specific internal API.
 *
 * The same stringval1 can appear N times — once per target_api that uses it.
 * This is intentional.
 */
@Data
@Entity
@Table(
    name = "partner_field_mapping",
    uniqueConstraints = @UniqueConstraint(columnNames = {"partner_code", "source_param", "target_api"})
)
public class PartnerFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_code", nullable = false, length = 20)
    private String partnerCode;

    @Column(name = "source_param", nullable = false, length = 20)
    private String sourceParam;     // stringval1 ... stringvalN

    @Column(name = "target_api", nullable = false, length = 50)
    private String targetApi;       // must match ApiName enum value

    @Column(name = "target_field", nullable = false, length = 100)
    private String targetField;     // field name in the downstream API request

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;        // STRING | DATE | DECIMAL | INTEGER | BOOLEAN

    @Column(name = "date_format", length = 30)
    private String dateFormat;      // e.g. dd/MM/yyyy — used only when dataType = DATE

    @Column(name = "is_mandatory")
    private boolean mandatory;

    @Column(name = "default_value", length = 200)
    private String defaultValue;    // used when source param is blank

    @Column(name = "transformation", length = 50)
    private String transformation;  // TRIM | UPPERCASE | NONE

    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    @Column(name = "is_active")
    private boolean active = true;

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
