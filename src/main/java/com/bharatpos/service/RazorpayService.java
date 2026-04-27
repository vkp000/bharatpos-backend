package com.bharatpos.service;

import com.bharatpos.entity.PaymentOrder;
import com.bharatpos.entity.Store;
import com.bharatpos.exception.BadRequestException;
import com.bharatpos.repository.PaymentOrderRepository;
import com.bharatpos.repository.StoreRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Service
@Slf4j
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final PaymentOrderRepository paymentOrderRepository;
    private final StoreRepository storeRepository;

    public RazorpayService(PaymentOrderRepository paymentOrderRepository,
                           StoreRepository storeRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.storeRepository = storeRepository;
    }

    // ── Create Payment Order ───────────────────────────────────────────
    public Map<String, Object> createOrder(Long storeId, BigDecimal amount,
                                           String invoiceNumber, String customerPhone,
                                           String customerName) {
        int amountPaise = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).intValue();

        // Simulated mode if placeholder keys
        if (isPlaceholder()) {
            String simulatedOrderId = "order_SIM_" + System.currentTimeMillis();
            PaymentOrder order = PaymentOrder.builder()
                    .razorpayOrderId(simulatedOrderId)
                    .amount(amount)
                    .status("created")
                    .invoiceNumber(invoiceNumber)
                    .customerPhone(customerPhone)
                    .customerName(customerName)
                    .build();

            storeRepository.findById(storeId).ifPresent(order::setStore);
            paymentOrderRepository.save(order);

            return Map.of(
                    "orderId", simulatedOrderId,
                    "amount", amountPaise,
                    "currency", "INR",
                    "keyId", "rzp_test_demo",
                    "simulated", true,
                    "qrData", "upi://pay?pa=bharatpos@ybl&pn=BharatPOS&am=" + amount + "&cu=INR&tn=" + invoiceNumber
            );
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", invoiceNumber);
            orderRequest.put("notes", new JSONObject()
                    .put("invoice", invoiceNumber)
                    .put("customer", customerName)
                    .put("phone", customerPhone));

            Order rzpOrder = client.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            PaymentOrder order = PaymentOrder.builder()
                    .razorpayOrderId(rzpOrderId)
                    .amount(amount)
                    .status("created")
                    .invoiceNumber(invoiceNumber)
                    .customerPhone(customerPhone)
                    .customerName(customerName)
                    .build();

            storeRepository.findById(storeId).ifPresent(order::setStore);
            paymentOrderRepository.save(order);

            return Map.of(
                    "orderId", rzpOrderId,
                    "amount", amountPaise,
                    "currency", "INR",
                    "keyId", keyId,
                    "simulated", false
            );

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new BadRequestException("Payment gateway error: " + e.getMessage());
        }
    }

    // ── Verify Payment Signature ───────────────────────────────────────
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Mark Payment as Paid ───────────────────────────────────────────
    public PaymentOrder markPaid(String razorpayOrderId, String paymentId, String signature) {
        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new BadRequestException("Order not found: " + razorpayOrderId));

        order.setRazorpayPaymentId(paymentId);
        order.setRazorpaySignature(signature);
        order.setStatus("paid");
        order.setPaidAt(LocalDateTime.now());

        return paymentOrderRepository.save(order);
    }

    // ── Verify Webhook Signature ───────────────────────────────────────
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPlaceholder() {
        return keyId == null || keyId.contains("placeholder") || keyId.equals("rzp_test_placeholder");
    }
}