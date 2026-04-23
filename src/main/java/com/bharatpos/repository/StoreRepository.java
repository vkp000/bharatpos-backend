package com.bharatpos.repository;

import com.bharatpos.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByTenantId(Long tenantId);
    Optional<Store> findByStoreCode(String storeCode);
}