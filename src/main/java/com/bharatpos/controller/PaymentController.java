package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.PaymentOrder;
import com.bharatpos.repository.PaymentOrderRepository;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.RazorpayService;
import com.bharatpos.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final RazorpayService razorpayService;
    private final WhatsAppService whatsAppService;
    private final PaymentOrderRepository paymentOrderRepository;

    // ── Create Razorpay Order (UPI QR) ────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @RequestBody Map<String, Object> body) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long tenantId = SecurityUtils.getCurrentTenantId();

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String invoiceNumber = (String) body.getOrDefault("invoiceNumber", "INV-0000");
        String customerPhone = (String) body.getOrDefault("customerPhone", "");
        String customerName = (String) body.getOrDefault("customerName", "Walk-in");

        Map<String, Object> orderData = razorpayService.createOrder(
                storeId, amount, invoiceNumber, customerPhone, customerName);

        return ResponseEntity.ok(ApiResponse.success(orderData));
    }

    // ── Verify Payment After Customer Pays ────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @RequestBody Map<String, String> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();

        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        // For simulated payments
        if (orderId != null && orderId.startsWith("order_SIM_")) {
            PaymentOrder order = razorpayService.markPaid(orderId, "pay_SIM_" + System.currentTimeMillis(), "sim_sig");
            // Send WhatsApp confirmation
            if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
                whatsAppService.sendPaymentConfirmation(
                        tenantId, order.getCustomerPhone(),
                        order.getCustomerName(), order.getInvoiceNumber(), order.getAmount());
            }
            return ResponseEntity.ok(ApiResponse.success(Map.of("verified", true, "status", "paid")));
        }

        boolean valid = razorpayService.verifyPaymentSignature(orderId, paymentId, signature);
        if (!valid) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Payment verification failed — invalid signature"));
        }

        PaymentOrder order = razorpayService.markPaid(orderId, paymentId, signature);

        // Auto-send WhatsApp confirmation
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().isBlank()) {
            whatsAppService.sendPaymentConfirmation(
                    tenantId, order.getCustomerPhone(),
                    order.getCustomerName(), order.getInvoiceNumber(), order.getAmount());
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "verified", true,
                "paymentId", paymentId,
                "status", "paid"
        )));
    }

    // ── Razorpay Webhook ──────────────────────────────────────────────
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("[WEBHOOK] Razorpay event received");

        if (signature != null && !razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("[WEBHOOK] Invalid signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            org.json.JSONObject event = new org.json.JSONObject(payload);
            String eventType = event.getString("event");
            log.info("[WEBHOOK] Event type: {}", eventType);

            if ("payment.captured".equals(eventType)) {
                org.json.JSONObject paymentEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String orderId = paymentEntity.optString("order_id");
                String paymentId = paymentEntity.getString("id");

                paymentOrderRepository.findByRazorpayOrderId(orderId).ifPresent(order -> {
                    if (!"paid".equals(order.getStatus())) {
                        order.setStatus("paid");
                        order.setRazorpayPaymentId(paymentId);
                        order.setPaidAt(java.time.LocalDateTime.now());
                        paymentOrderRepository.save(order);
                        log.info("[WEBHOOK] Order {} marked as paid", orderId);
                    }
                });
            }
        } catch (Exception e) {
            log.error("[WEBHOOK] Processing error: {}", e.getMessage());
        }

        return ResponseEntity.ok("OK");
    }

    // ── Get Payment Orders ─────────────────────────────────────────────
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<PaymentOrder>>> getOrders() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(
                paymentOrderRepository.findByStoreIdOrderByCreatedAtDesc(storeId)));
    }

    // ── Check Payment Status ───────────────────────────────────────────
    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse<PaymentOrder>> getStatus(@PathVariable String orderId) {
        return paymentOrderRepository.findByRazorpayOrderId(orderId)
                .map(order -> ResponseEntity.ok(ApiResponse.success(order)))
                .orElse(ResponseEntity.notFound().build());
    }
}