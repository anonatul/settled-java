package com.settled.repository;

import com.settled.domain.Claim;
import com.settled.domain.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Page<Claim> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Claim> findByCustomerIdAndStatus(UUID customerId, ClaimStatus status, Pageable pageable);

    boolean existsByClaimNumber(String claimNumber);

    Optional<Claim> findByClaimNumber(String claimNumber);

    long countByCustomerId(UUID customerId);

    long countByCustomerIdAndStatus(UUID customerId, ClaimStatus status);

    long countByStatus(ClaimStatus status);

    @Query("""
            select c from Claim c
            where (:q is null or lower(c.claimNumber) like lower(concat('%', cast(:q as text), '%'))
               or lower(c.customer.user.email) like lower(concat('%', cast(:q as text), '%')))
              and (:status is null or c.status = :status)
            order by c.submittedAt desc
            """)
    Page<Claim> search(@Param("q") String q, @Param("status") ClaimStatus status, Pageable pageable);

    @Query("""
            select distinct c from Claim c
            join ClaimAssignment ca on ca.claim = c
            where ca.officer.id = :officerId and ca.active = true
              and (:status is null or c.status = :status)
            order by c.submittedAt desc
            """)
    Page<Claim> findByActiveAssignmentOfficer(@Param("officerId") UUID officerId,
                                              @Param("status") ClaimStatus status,
                                              Pageable pageable);

    @Query("""
            select c.status, count(c) from Claim c group by c.status
            """)
    List<Object[]> countByStatusGrouped();

    @Query("""
            select function('to_char', c.submittedAt, 'YYYY-MM'), count(c)
            from Claim c
            where c.submittedAt >= :since
            group by function('to_char', c.submittedAt, 'YYYY-MM')
            order by function('to_char', c.submittedAt, 'YYYY-MM')
            """)
    List<Object[]> countByMonth(@Param("since") java.time.Instant since);
}