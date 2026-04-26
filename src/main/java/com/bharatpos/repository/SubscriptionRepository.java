package com.bharatpos.repository;

import com.bharatpos.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByTenantId(Long tenantId);

    @Modifying
    @Query("UPDATE Subscription s SET s.invoiceCount = s.invoiceCount + 1 WHERE s.tenant.id = :tenantId")
    void incrementInvoiceCount(Long tenantId);
}