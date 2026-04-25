package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Inventory;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.InventoryRepository;
import com.bharatpos.repository.ProductRepository;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryStockController {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Inventory>>> getStoreInventory() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(
                inventoryRepository.findByStoreId(storeId)));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<Inventory>>> getLowStock() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(
                inventoryRepository.findLowStockByStore(storeId)));
    }

    @PutMapping("/adjust")
    public ResponseEntity<ApiResponse<Inventory>> adjustStock(
            @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Long storeId = SecurityUtils.getCurrentStoreId();
        Integer qty = Integer.valueOf(body.get("quantity").toString());

        Inventory inv = inventoryRepository.findByProductIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory record not found for product: " + productId));
        inv.setQuantity(qty);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted",
                inventoryRepository.save(inv)));
    }
}