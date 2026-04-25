package com.bharatpos.controller;

import com.bharatpos.dto.request.CreateSaleRequest;
import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.dto.response.SaleResponse;
import com.bharatpos.entity.Sale;
import com.bharatpos.entity.SaleItem;
import com.bharatpos.repository.SaleItemRepository;
import com.bharatpos.repository.SaleRepository;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

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
    public ResponseEntity<ApiResponse<List<Sale>>> getRecentSales() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(
                saleRepository.findTop10ByStoreIdOrderByCreatedAtDesc(storeId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Sale>>> getAllSales(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        List<Sale> sales;
        if (from != null && to != null) {
            LocalDateTime fromDt = LocalDateTime.parse(from + "T00:00:00");
            LocalDateTime toDt = LocalDateTime.parse(to + "T23:59:59");
            sales = saleRepository.findByStoreIdAndDateRange(storeId, fromDt, toDt);
        } else {
            sales = saleRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        }
        return ResponseEntity.ok(ApiResponse.success(sales));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSaleById(@PathVariable Long id) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new com.bharatpos.exception.ResourceNotFoundException("Sale", id));
        List<SaleItem> items = saleItemRepository.findBySaleId(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("sale", sale, "items", items)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        LocalDateTime todayStart = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        var todayRevenue = saleRepository.sumRevenueByStoreAndDateRange(storeId, todayStart, todayEnd);
        var todayCount = saleRepository.countSalesByStoreAndDateRange(storeId, todayStart, todayEnd);
        var monthRevenue = saleRepository.sumRevenueByStoreAndDateRange(storeId, monthStart, todayEnd);
        var todayGST = saleRepository.sumTaxByStoreAndDateRange(storeId, todayStart, todayEnd);

        var paymentBreakdown = saleRepository.getPaymentModeBreakdown(storeId, todayStart, todayEnd);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "todayRevenue", todayRevenue,
                "todayCount", todayCount,
                "monthRevenue", monthRevenue,
                "todayGST", todayGST,
                "paymentBreakdown", paymentBreakdown
        )));
    }
}