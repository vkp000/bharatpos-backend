package com.bharatpos.repository;

import com.bharatpos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTenantIdAndActiveTrue(Long tenantId);

    Optional<Product> findByBarcodeAndTenantId(String barcode, Long tenantId);

    Optional<Product> findBySkuAndTenantId(String sku, Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "p.barcode LIKE CONCAT('%', :query, '%') OR " +
            "p.sku LIKE CONCAT('%', :query, '%')) AND p.active = true")
    List<Product> searchProducts(Long tenantId, String query);
}