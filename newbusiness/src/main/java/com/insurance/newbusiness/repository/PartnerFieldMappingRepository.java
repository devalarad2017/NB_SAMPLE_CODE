package com.insurance.newbusiness.repository;
import com.insurance.newbusiness.domain.entity.PartnerFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PartnerFieldMappingRepository extends JpaRepository<PartnerFieldMapping, Long> {
    List<PartnerFieldMapping> findByActiveTrue();
}
