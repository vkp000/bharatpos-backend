package com.bharatpos.repository;

import com.bharatpos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<Sale> findTop10ByStoreIdOrderByCreatedAtDesc(Long storeId);

    @Query("SELECT s FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end " +
            "AND s.status = 'COMPLETED' ORDER BY s.createdAt DESC")
    List<Sale> findByStoreIdAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    BigDecimal sumRevenueByStoreAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    Long countSalesByStoreAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.taxAmount), 0) FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    BigDecimal sumTaxByStoreAndDateRange(Long storeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s.paymentMode as mode, COUNT(s) as count, SUM(s.grandTotal) as total " +
            "FROM Sale s WHERE s.store.id = :storeId " +
            "AND s.createdAt BETWEEN :start AND :end AND s.status = 'COMPLETED' " +
            "GROUP BY s.paymentMode")
    List<Map<String, Object>> getPaymentModeBreakdown(Long storeId, LocalDateTime start, LocalDateTime end);

    boolean existsByInvoiceNumber(String invoiceNumber);
}