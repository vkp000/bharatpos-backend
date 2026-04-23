package com.bharatpos.controller;

import com.bharatpos.dto.request.CreateSaleRequest;
import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.dto.response.SaleResponse;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponse>> createSale(
            @Valid @RequestBody CreateSaleRequest request) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        SaleResponse response = saleService.createSale(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Sale created successfully", response));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<?>> getRecentSales() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(saleService.getRecentSales(storeId)));
    }
}