package com.settled.repository;

import com.settled.domain.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Page<Policy> findByCustomerId(UUID customerId, Pageable pageable);

    boolean existsByPolicyNumber(String policyNumber);

    Optional<Policy> findByPolicyNumber(String policyNumber);

    @Query("""
            select p from Policy p
            where (:q is null or lower(p.policyNumber) like lower(concat('%', cast(:q as text), '%'))
               or lower(p.customer.user.email) like lower(concat('%', cast(:q as text), '%'))
               or lower(p.customer.customerNumber) like lower(concat('%', cast(:q as text), '%')))
              and (:status is null or p.status = :status)
            """)
    Page<Policy> search(@Param("q") String q, @Param("status") Object status, Pageable pageable);

    long countByCustomerId(UUID customerId);
}