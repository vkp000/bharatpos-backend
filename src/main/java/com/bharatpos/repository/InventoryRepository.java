package com.bharatpos.repository;

import com.bharatpos.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductIdAndStoreId(Long productId, Long storeId);

    List<Inventory> findByStoreId(Long storeId);

    @Query("SELECT i FROM Inventory i JOIN i.product p WHERE i.store.id = :storeId " +
            "AND i.quantity <= p.reorderLevel AND p.active = true")
    List<Inventory> findLowStockByStore(Long storeId);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :qty WHERE i.product.id = :productId AND i.store.id = :storeId")
    int decrementStock(Long productId, Long storeId, Integer qty);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :qty WHERE i.product.id = :productId AND i.store.id = :storeId")
    int incrementStock(Long productId, Long storeId, Integer qty);
}