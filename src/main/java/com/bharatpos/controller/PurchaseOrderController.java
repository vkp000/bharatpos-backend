package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.PurchaseOrder;
import com.bharatpos.entity.Store;
import com.bharatpos.entity.Supplier;
import com.bharatpos.entity.User;
import com.bharatpos.enums.POStatus;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.PurchaseOrderRepository;
import com.bharatpos.repository.StoreRepository;
import com.bharatpos.repository.SupplierRepository;
import com.bharatpos.repository.UserRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderRepository poRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getAll() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(
                poRepository.findByStoreIdOrderByCreatedAtDesc(storeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrder>> create(@RequestBody Map<String, Object> body) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.getCurrentUserId();
        Long tenantId = SecurityUtils.getCurrentTenantId();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Find supplier by name
        String supplierName = (String) body.get("supplierName");
        Supplier supplier = supplierRepository.findByTenantIdAndActiveTrue(tenantId)
                .stream()
                .filter(s -> s.getName().equals(supplierName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierName));

        String poNumber = "PO-" + System.currentTimeMillis();

        PurchaseOrder po = PurchaseOrder.builder()
                .store(store)
                .supplier(supplier)
                .createdBy(user)
                .poNumber(poNumber)
                .totalItems(body.get("totalItems") != null ? ((Number) body.get("totalItems")).intValue() : 1)
                .totalAmount(new BigDecimal(body.get("totalAmount").toString()))
                .expectedDelivery(body.get("expectedDelivery") != null
                        ? LocalDate.parse((String) body.get("expectedDelivery")) : null)
                .status(POStatus.DRAFT)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PO created", poRepository.save(po)));
    }
}