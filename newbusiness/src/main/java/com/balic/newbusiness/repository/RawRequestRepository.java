package com.balic.newbusiness.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.balic.newbusiness.domain.entity.RawRequest;

public interface RawRequestRepository extends JpaRepository<RawRequest, Long> {}
