package com.bharatpos.repository;

import com.bharatpos.entity.StockAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAuditLogRepository extends JpaRepository<StockAuditLog, Long> {
    List<StockAuditLog> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<StockAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId);
}