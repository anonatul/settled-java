package com.settled.repository;

import com.settled.domain.User;
import com.settled.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);

    @Query("""
            select u from User u
            where (:q is null or lower(u.email) like lower(concat('%', cast(:q as text), '%'))
               or lower(u.firstName) like lower(concat('%', cast(:q as text), '%'))
               or lower(u.lastName) like lower(concat('%', cast(:q as text), '%')))
              and (:role is null or u.role = :role)
            """)
    Page<User> search(@Param("q") String q, @Param("role") Role role, Pageable pageable);
}