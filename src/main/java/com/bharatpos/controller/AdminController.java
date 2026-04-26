package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long totalTenants = tenantRepository.count();
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalCustomers = customerRepository.count();
        long totalSales = saleRepository.count();

        LocalDateTime monthStart = LocalDateTime.now()
                .truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
        LocalDateTime now = LocalDateTime.now();

        // MRR calculation
        var subscriptions = subscriptionRepository.findAll();
        BigDecimal mrr = subscriptions.stream()
                .filter(s -> s.getMonthlyAmount() != null)
                .map(s -> s.getMonthlyAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Plan distribution
        Map<String, Long> planDist = subscriptions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getPlan() != null ? s.getPlan() : "starter",
                        Collectors.counting()));

        // New tenants this month
        long newTenantsThisMonth = tenantRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null &&
                        t.getCreatedAt().isAfter(monthStart))
                .count();

        Map<String, Object> result = new HashMap<>();
        result.put("totalTenants", totalTenants);
        result.put("totalUsers", totalUsers);
        result.put("totalProducts", totalProducts);
        result.put("totalCustomers", totalCustomers);
        result.put("totalSales", totalSales);
        result.put("mrr", mrr);
        result.put("planDistribution", planDist);
        result.put("newTenantsThisMonth", newTenantsThisMonth);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<?>> getTenants() {
        var tenants = tenantRepository.findAll().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("businessName", t.getBusinessName());
            m.put("phone", t.getPhone());
            m.put("email", t.getEmail());
            m.put("plan", t.getPlan());
            m.put("active", t.getActive());
            m.put("createdAt", t.getCreatedAt());
            var sub = subscriptionRepository.findByTenantId(t.getId());
            m.put("invoiceCount", sub.map(s -> s.getInvoiceCount()).orElse(0));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }
}