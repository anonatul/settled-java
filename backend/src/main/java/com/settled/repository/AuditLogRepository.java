package com.settled.repository;

import com.settled.domain.AuditLog;
import com.settled.domain.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a from AuditLog a
            where (:q is null or lower(a.actor.email) like lower(concat('%', cast(:q as text), '%')))
              and (:action is null or a.action = :action)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(@Param("q") String q, @Param("action") AuditAction action, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);
}