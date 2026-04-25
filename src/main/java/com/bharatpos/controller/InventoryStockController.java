package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.*;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.*;
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
    private final UserRepository userRepository;
    private final StockAuditLogRepository auditLogRepository;

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

    @GetMapping("/audit-log")
    public ResponseEntity<ApiResponse<List<StockAuditLog>>> getAuditLog() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(
                auditLogRepository.findByStoreIdOrderByCreatedAtDesc(storeId)));
    }

    @PutMapping("/adjust")
    public ResponseEntity<ApiResponse<Inventory>> adjustStock(
            @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.getCurrentUserId();
        Integer newQty = Integer.valueOf(body.get("quantity").toString());
        String reason = (String) body.getOrDefault("reason", "Manual adjustment");
        String type = (String) body.getOrDefault("type", "ADJUSTMENT");

        Inventory inv = inventoryRepository.findByProductIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product: " + productId));

        int before = inv.getQuantity();
        inv.setQuantity(newQty);
        inventoryRepository.save(inv);

        // Log the adjustment
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        User user = userRepository.findById(userId).orElse(null);

        StockAuditLog log = StockAuditLog.builder()
                .product(product)
                .store(store)
                .user(user)
                .quantityBefore(before)
                .quantityAfter(newQty)
                .change(newQty - before)
                .reason(reason)
                .type(type)
                .build();
        auditLogRepository.save(log);

        return ResponseEntity.ok(ApiResponse.success("Stock adjusted", inv));
    }

    @PutMapping("/adjust-bulk")
    public ResponseEntity<ApiResponse<String>> adjustBulk(
            @RequestBody List<Map<String, Object>> items) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.getCurrentUserId();
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        User user = userRepository.findById(userId).orElse(null);

        for (Map<String, Object> item : items) {
            Long productId = Long.valueOf(item.get("productId").toString());
            Integer newQty = Integer.valueOf(item.get("quantity").toString());
            String reason = (String) item.getOrDefault("reason", "Bulk adjustment");

            inventoryRepository.findByProductIdAndStoreId(productId, storeId)
                    .ifPresent(inv -> {
                        int before = inv.getQuantity();
                        inv.setQuantity(newQty);
                        inventoryRepository.save(inv);

                        productRepository.findById(productId).ifPresent(product -> {
                            StockAuditLog log = StockAuditLog.builder()
                                    .product(product).store(store).user(user)
                                    .quantityBefore(before).quantityAfter(newQty)
                                    .change(newQty - before).reason(reason).type("ADJUSTMENT")
                                    .build();
                            auditLogRepository.save(log);
                        });
                    });
        }
        return ResponseEntity.ok(ApiResponse.success("Bulk stock adjusted", null));
    }
}