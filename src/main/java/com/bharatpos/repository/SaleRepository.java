package com.bharatpos.repository;

import com.bharatpos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<Sale> findTop10ByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    BigDecimal sumRevenueByStoreAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    Long countSalesByStoreAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.taxAmount), 0) FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    BigDecimal sumTaxByStoreAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);
}