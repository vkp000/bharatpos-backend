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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;

    private static final Map<String, Map<String, Object>> PLANS = Map.of(
            "starter", Map.of(
                    "name", "Starter",
                    "price", 0,
                    "maxInvoices", 500,
                    "maxStores", 1,
                    "maxUsers", 1,
                    "features", new String[]{"POS & Billing", "Basic Inventory", "GST Reports"}
            ),
            "basic", Map.of(
                    "name", "Basic",
                    "price", 999,
                    "maxInvoices", -1,
                    "maxStores", 1,
                    "maxUsers", 3,
                    "features", new String[]{"Everything in Starter", "WhatsApp Invoices", "Customer CRM", "Supplier Management"}
            ),
            "growth", Map.of(
                    "name", "Growth",
                    "price", 2499,
                    "maxInvoices", -1,
                    "maxStores", 3,
                    "maxUsers", 10,
                    "features", new String[]{"Everything in Basic", "Multi-Store", "e-Invoice", "AI Forecasting"}
            ),
            "pro", Map.of(
                    "name", "Pro",
                    "price", 4999,
                    "maxInvoices", -1,
                    "maxStores", 10,
                    "maxUsers", -1,
                    "features", new String[]{"Everything in Growth", "API Access", "Priority Support"}
            )
    );

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscription() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Subscription sub = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultSubscription(tenantId));

        Map<String, Object> result = Map.of(
                "subscription", sub,
                "plans", PLANS,
                "currentPlan", PLANS.getOrDefault(sub.getPlan(), PLANS.get("starter")),
                "usagePercent", sub.getMaxInvoices() > 0
                        ? (sub.getInvoiceCount() * 100.0 / sub.getMaxInvoices()) : 0
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlans() {
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
        sub.setMaxUsers((Integer) planDetails.get("maxUsers"));
        sub.setMonthlyAmount(BigDecimal.valueOf((Integer) planDetails.get("price")));
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusMonths(1));
        sub.setStatus("active");

        // Update tenant plan
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

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "canBill", canBill,
                "invoiceCount", sub.getInvoiceCount(),
                "maxInvoices", sub.getMaxInvoices(),
                "remaining", remaining,
                "plan", sub.getPlan()
        )));
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