package com.bharatpos.repository;

import com.bharatpos.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhoneAndTenantId(String phone, Long tenantId);

    List<Customer> findByTenantId(Long tenantId);

    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phone LIKE CONCAT('%', :query, '%'))")
    List<Customer> searchCustomers(Long tenantId, String query);
}