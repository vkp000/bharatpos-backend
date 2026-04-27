package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Customer;
import com.bharatpos.entity.WhatsAppMessage;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.CustomerRepository;
import com.bharatpos.repository.WhatsAppMessageRepository;
import com.bharatpos.security.SecurityUtils;
import com.bharatpos.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppMessageRepository messageRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<WhatsAppMessage>>> getMessages() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return ResponseEntity.ok(ApiResponse.success(
                messageRepository.findByTenantIdOrderBySentAtDesc(tenantId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        long sent = messageRepository.countByTenantIdAndStatus(tenantId, "sent");
        long simulated = messageRepository.countByTenantIdAndStatus(tenantId, "simulated");
        long failed = messageRepository.countByTenantIdAndStatus(tenantId, "failed");
        long total = messageRepository.findByTenantIdOrderBySentAtDesc(tenantId).size();

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "total", total,
                "sent", sent,
                "simulated", simulated,
                "failed", failed,
                "deliveryRate", total > 0 ? Math.round(((double)(sent + simulated) / total) * 100) + "%" : "0%"
        )));
    }

    @PostMapping("/send-invoice")
    public ResponseEntity<ApiResponse<String>> sendInvoice(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        String phone = (String) body.get("phone");
        String name = (String) body.get("name");
        String invoiceNumber = (String) body.get("invoiceNumber");
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
        String paymentMode = (String) body.getOrDefault("paymentMode", "UPI");

        whatsAppService.sendInvoice(tenantId, phone, name, invoiceNumber, amount, paymentMode);
        return ResponseEntity.ok(ApiResponse.success("Invoice sending initiated", null));
    }

    @PostMapping("/send-reminder")
    public ResponseEntity<ApiResponse<String>> sendReminder(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long customerId = Long.valueOf(body.get("customerId").toString());

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
        String invoiceNumber = (String) body.getOrDefault("invoiceNumber", "");

        whatsAppService.sendPaymentReminder(
                tenantId, customer.getPhone(), customer.getName(), amount, invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success("Reminder sent", null));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<String>> broadcast(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        String segment = (String) body.getOrDefault("segment", "all");
        String message = (String) body.get("message");

        List<Customer> customers = customerRepository.findByTenantId(tenantId);
        List<String> phones = customers.stream()
                .filter(c -> {
                    if ("all".equals(segment)) return true;
                    return segment.equals(c.getSegment());
                })
                .map(Customer::getPhone)
                .filter(p -> p != null && !p.isBlank())
                .toList();

        whatsAppService.sendBroadcast(tenantId, phones, message, "BROADCAST");

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Broadcast initiated to %d customers", phones.size()), null));
    }

    @PostMapping("/send-loyalty")
    public ResponseEntity<ApiResponse<String>> sendLoyalty(@RequestBody Map<String, Object> body) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Long customerId = Long.valueOf(body.get("customerId").toString());

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        int pointsEarned = Integer.parseInt(body.getOrDefault("pointsEarned", "0").toString());

        whatsAppService.sendLoyaltyUpdate(
                tenantId, customer.getPhone(), customer.getName(),
                pointsEarned, customer.getLoyaltyPoints());
        return ResponseEntity.ok(ApiResponse.success("Loyalty message sent", null));
    }

    // Meta webhook verification
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && "bharatpos_verify_token".equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.badRequest().body("Verification failed");
    }

    // Meta webhook incoming messages
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody String payload) {
        // Log incoming messages — full chatbot implementation can be added here
        return ResponseEntity.ok("OK");
    }
}