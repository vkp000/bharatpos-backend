package com.bharatpos.repository;

import com.bharatpos.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByGstin(String gstin);
    boolean existsByGstin(String gstin);
}