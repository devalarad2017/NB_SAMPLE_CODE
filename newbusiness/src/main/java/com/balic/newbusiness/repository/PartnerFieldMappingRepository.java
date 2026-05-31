package com.balic.newbusiness.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.balic.newbusiness.domain.entity.PartnerFieldMapping;

import java.util.List;

public interface PartnerFieldMappingRepository extends JpaRepository<PartnerFieldMapping, Long> {
    List<PartnerFieldMapping> findByActiveTrue();
}
