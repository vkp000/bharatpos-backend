package com.bharatpos.repository;

import com.bharatpos.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    boolean existsByPoNumber(String poNumber);
}