package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long tenantId = SecurityUtils.getCurrentTenantId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        // Revenue data
        BigDecimal todayRev = saleRepository.sumRevenueByStoreAndDateRange(storeId, todayStart, todayEnd);
        BigDecimal weekRev = saleRepository.sumRevenueByStoreAndDateRange(storeId, weekStart, todayEnd);
        BigDecimal monthRev = saleRepository.sumRevenueByStoreAndDateRange(storeId, monthStart, todayEnd);
        Long todayCount = saleRepository.countSalesByStoreAndDateRange(storeId, todayStart, todayEnd);
        BigDecimal todayGST = saleRepository.sumTaxByStoreAndDateRange(storeId, todayStart, todayEnd);

        // Payment breakdown
        var paymentBreakdown = saleRepository.getPaymentModeBreakdown(storeId, monthStart, todayEnd);

        // Monthly trend (last 6 months)
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime mStart = todayStart.minusMonths(i).withDayOfMonth(1);
            LocalDateTime mEnd = mStart.plusMonths(1);
            BigDecimal mRev = saleRepository.sumRevenueByStoreAndDateRange(storeId, mStart, mEnd);
            Long mCount = saleRepository.countSalesByStoreAndDateRange(storeId, mStart, mEnd);
            Map<String, Object> m = new HashMap<>();
            m.put("month", mStart.getMonth().toString().substring(0, 3));
            m.put("revenue", mRev);
            m.put("bills", mCount);
            monthlyTrend.add(m);
        }

        // Customer count
        long totalCustomers = customerRepository.findByTenantId(tenantId).size();

        // Low stock count
        long lowStockCount = inventoryRepository.findLowStockByStore(storeId).size();

        Map<String, Object> result = new HashMap<>();
        result.put("todayRevenue", todayRev);
        result.put("todayBills", todayCount);
        result.put("todayGST", todayGST);
        result.put("weekRevenue", weekRev);
        result.put("monthRevenue", monthRev);
        result.put("totalCustomers", totalCustomers);
        result.put("lowStockCount", lowStockCount);
        result.put("paymentBreakdown", paymentBreakdown);
        result.put("monthlyTrend", monthlyTrend);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}