package com.settled.repository;

import com.settled.domain.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyTypeRepository extends JpaRepository<PolicyType, UUID> {

    Optional<PolicyType> findByCode(String code);

    boolean existsByCode(String code);

    List<PolicyType> findByActiveTrueOrderByNameAsc();
}