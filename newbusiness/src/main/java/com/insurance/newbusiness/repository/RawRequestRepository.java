package com.insurance.newbusiness.repository;
import com.insurance.newbusiness.domain.entity.RawRequest;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RawRequestRepository extends JpaRepository<RawRequest, Long> {}
