package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Subscription;
import com.bharatpos.entity.Tenant;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.SubscriptionRepository;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;

    private static Map<String, Object> buildPlan(String name, int price,
                                                 int maxInvoices, int maxStores, Object maxUsers, String... features) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("name", name);
        plan.put("price", price);
        plan.put("maxInvoices", maxInvoices);
        plan.put("maxStores", maxStores);
        plan.put("maxUsers", maxUsers);
        plan.put("features", features);
        return plan;
    }

    private static final Map<String, Map<String, Object>> PLANS;

    static {
        PLANS = new HashMap<>();
        PLANS.put("starter", buildPlan("Starter", 0, 500, 1, 1,
                "POS & Billing", "Basic Inventory", "GST Reports"));
        PLANS.put("basic", buildPlan("Basic", 999, -1, 1, 3,
                "Everything in Starter", "WhatsApp Invoices",
                "Customer CRM", "Supplier Management"));
        PLANS.put("growth", buildPlan("Growth", 2499, -1, 3, 10,
                "Everything in Basic", "Multi-Store", "e-Invoice", "AI Forecasting"));
        PLANS.put("pro", buildPlan("Pro", 4999, -1, 10, "Unlimited",
                "Everything in Growth", "API Access", "Priority Support"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscription() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        double usagePercent = sub.getMaxInvoices() > 0
                ? (sub.getInvoiceCount() * 100.0 / sub.getMaxInvoices()) : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("subscription", sub);
        result.put("plans", PLANS);
        result.put("currentPlan", PLANS.getOrDefault(sub.getPlan(), PLANS.get("starter")));
        result.put("usagePercent", usagePercent);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(PLANS));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse<Subscription>> upgrade(
            @RequestBody Map<String, String> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        String newPlan = body.get("plan");

        if (!PLANS.containsKey(newPlan)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid plan: " + newPlan));
        }

        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        Map<String, Object> planDetails = PLANS.get(newPlan);
        sub.setPlan(newPlan);
        sub.setMaxInvoices((Integer) planDetails.get("maxInvoices"));
        sub.setMaxStores((Integer) planDetails.get("maxStores"));

        Object maxUsers = planDetails.get("maxUsers");
        if (maxUsers instanceof Integer) {
            sub.setMaxUsers((Integer) maxUsers);
        } else {
            sub.setMaxUsers(-1);
        }

        sub.setMonthlyAmount(BigDecimal.valueOf((Integer) planDetails.get("price")));
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
        sub.setStatus("active");

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        tenant.setPlan(newPlan);
        tenantRepository.save(tenant);

        return ResponseEntity.ok(ApiResponse.success(
                "Plan upgraded to " + newPlan, subscriptionRepository.save(sub)));
    }

    @GetMapping("/check-limit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkLimit() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        boolean canBill = sub.getMaxInvoices() < 0 ||
                sub.getInvoiceCount() < sub.getMaxInvoices();
        int remaining = sub.getMaxInvoices() < 0
                ? -1 : sub.getMaxInvoices() - sub.getInvoiceCount();

        Map<String, Object> result = new HashMap<>();
        result.put("canBill", canBill);
        result.put("invoiceCount", sub.getInvoiceCount());
        result.put("maxInvoices", sub.getMaxInvoices());
        result.put("remaining", remaining);
        result.put("plan", sub.getPlan());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Subscription createDefaultSubscription(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        Subscription sub = Subscription.builder()
                .tenant(tenant)
                .plan("starter")
                .status("active")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(10))
                .maxInvoices(500)
                .maxStores(1)
                .maxUsers(1)
                .invoiceCount(0)
                .monthlyAmount(BigDecimal.ZERO)
                .build();
        return subscriptionRepository.save(sub);
    }
}