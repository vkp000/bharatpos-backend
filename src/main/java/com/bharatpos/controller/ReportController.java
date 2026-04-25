package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Sale;
import com.bharatpos.repository.CustomerRepository;
import com.bharatpos.repository.InventoryRepository;
import com.bharatpos.repository.SaleRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesReport(
            @RequestParam(defaultValue = "today") String period) {

        Long storeId = SecurityUtils.getCurrentStoreId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end = now;

        switch (period) {
            case "today" -> start = now.truncatedTo(ChronoUnit.DAYS);
            case "week" -> start = now.truncatedTo(ChronoUnit.DAYS).minusDays(7);
            case "month" -> start = now.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            case "quarter" -> start = now.truncatedTo(ChronoUnit.DAYS).minusMonths(3);
            case "year" -> start = now.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            default -> start = now.truncatedTo(ChronoUnit.DAYS);
        }

        List<Sale> sales = saleRepository.findByStoreIdAndDateRange(storeId, start, end);

        BigDecimal totalRevenue = sales.stream()
                .map(Sale::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = sales.stream()
                .map(Sale::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscount = sales.stream()
                .map(Sale::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Payment breakdown
        Map<String, Long> paymentCount = sales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getPaymentMode().name(), Collectors.counting()));
        Map<String, BigDecimal> paymentAmount = sales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getPaymentMode().name(),
                        Collectors.reducing(BigDecimal.ZERO,
                                Sale::getGrandTotal, BigDecimal::add)));

        // Daily breakdown
        Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        sales.forEach(s -> {
            if (s.getCreatedAt() != null) {
                String day = s.getCreatedAt().toLocalDate().toString();
                dailyRevenue.merge(s.getCreatedAt().toLocalDate().toString(),
                        s.getGrandTotal(), BigDecimal::add);
            }
        });

        Map<String, Object> report = new HashMap<>();
        report.put("period", period);
        report.put("totalSales", sales.size());
        report.put("totalRevenue", totalRevenue);
        report.put("totalTax", totalTax);
        report.put("totalDiscount", totalDiscount);
        report.put("avgOrderValue", sales.isEmpty() ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(sales.size()), 2,
                java.math.RoundingMode.HALF_UP));
        report.put("paymentCount", paymentCount);
        report.put("paymentAmount", paymentAmount);
        report.put("dailyRevenue", dailyRevenue);
        report.put("startDate", start.toLocalDate().toString());
        report.put("endDate", end.toLocalDate().toString());

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventoryReport() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        var inventory = inventoryRepository.findByStoreId(storeId);
        var lowStock = inventoryRepository.findLowStockByStore(storeId);

        int totalQty = inventory.stream()
                .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
        BigDecimal totalValue = inventory.stream()
                .map(i -> {
                    BigDecimal cost = i.getProduct().getCostPrice();
                    int qty = i.getQuantity() != null ? i.getQuantity() : 0;
                    return cost != null ? cost.multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO;
                }).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("totalSkus", inventory.size());
        report.put("totalQuantity", totalQty);
        report.put("totalValue", totalValue);
        report.put("outOfStock", inventory.stream().filter(i -> i.getQuantity() == 0).count());
        report.put("lowStockCount", lowStock.size());
        report.put("lowStockItems", lowStock.stream().map(inv -> Map.of(
                "name", inv.getProduct().getName(),
                "currentStock", inv.getQuantity(),
                "reorderLevel", inv.getProduct().getReorderLevel() != null
                        ? inv.getProduct().getReorderLevel() : 0,
                "category", inv.getProduct().getCategory() != null
                        ? inv.getProduct().getCategory() : "—"
        )).collect(Collectors.toList()));

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerReport() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        var customers = customerRepository.findByTenantId(tenantId);

        Map<String, Long> bySegment = customers.stream()
                .filter(c -> c.getSegment() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getSegment(), Collectors.counting()));

        BigDecimal totalLTV = customers.stream()
                .map(c -> c.getTotalSpend() != null ? c.getTotalSpend() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalPoints = customers.stream()
                .mapToInt(c -> c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0).sum();

        Map<String, Object> report = new HashMap<>();
        report.put("totalCustomers", customers.size());
        report.put("bySegment", bySegment);
        report.put("totalLTV", totalLTV);
        report.put("avgLTV", customers.isEmpty() ? BigDecimal.ZERO
                : totalLTV.divide(BigDecimal.valueOf(customers.size()), 2,
                java.math.RoundingMode.HALF_UP));
        report.put("totalLoyaltyPoints", totalPoints);
        report.put("repeatCustomers",
                customers.stream().filter(c -> c.getTotalVisits() != null
                        && c.getTotalVisits() > 1).count());

        return ResponseEntity.ok(ApiResponse.success(report));
    }
}