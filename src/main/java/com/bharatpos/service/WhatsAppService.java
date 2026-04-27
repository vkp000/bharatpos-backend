package com.bharatpos.service;

import com.bharatpos.entity.Tenant;
import com.bharatpos.entity.WhatsAppMessage;
import com.bharatpos.repository.TenantRepository;
import com.bharatpos.repository.WhatsAppMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final WhatsAppMessageRepository messageRepository;
    private final TenantRepository tenantRepository;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.api.token}")
    private String apiToken;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    private final WebClient webClient = WebClient.builder().build();

    // ── Send Invoice via WhatsApp ──────────────────────────────────────
    @Async
    public void sendInvoice(Long tenantId, String customerPhone, String customerName,
                            String invoiceNumber, BigDecimal amount, String paymentMode) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, "INVOICE",
                    String.format("Invoice %s for ₹%s sent to %s", invoiceNumber, amount, customerName));
            return;
        }

        String message = String.format(
                "Hello %s! 🛍️\n\nYour invoice *#%s* has been generated.\n\n" +
                        "💰 Amount: *₹%s*\n💳 Payment: *%s*\n\n" +
                        "Thank you for shopping with us! 🙏\n\n_Powered by BharatPOS_",
                customerName, invoiceNumber,
                amount.toLocaleString(), paymentMode
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "INVOICE", invoiceNumber);
    }

    // ── Send Payment Confirmation ──────────────────────────────────────
    @Async
    public void sendPaymentConfirmation(Long tenantId, String customerPhone,
                                        String customerName, String invoiceNumber,
                                        BigDecimal amount) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, "PAYMENT_CONFIRM",
                    String.format("Payment confirmed ₹%s for %s", amount, customerName));
            return;
        }

        String message = String.format(
                "✅ Payment Confirmed!\n\nHello %s,\n\n" +
                        "We've received your payment of *₹%s* for invoice *#%s*.\n\n" +
                        "Thank you for your business! Visit us again soon. 🙏",
                customerName, amount, invoiceNumber
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "PAYMENT_CONFIRM", invoiceNumber);
    }

    // ── Send Loyalty Points Update ─────────────────────────────────────
    @Async
    public void sendLoyaltyUpdate(Long tenantId, String customerPhone,
                                  String customerName, int pointsEarned,
                                  int totalPoints) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, "LOYALTY",
                    String.format("Loyalty: +%d pts, total %d for %s", pointsEarned, totalPoints, customerName));
            return;
        }

        String message = String.format(
                "⭐ Points Earned!\n\nHi %s,\n\n" +
                        "You earned *%d points* on your last purchase!\n\n" +
                        "💎 Total Points: *%d*\n" +
                        "💰 Value: *₹%d* off your next purchase\n\n" +
                        "Keep shopping to earn more rewards! 🛍️",
                customerName, pointsEarned, totalPoints, totalPoints / 10
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "LOYALTY", null);
    }

    // ── Send Payment Reminder (Khata) ──────────────────────────────────
    @Async
    public void sendPaymentReminder(Long tenantId, String customerPhone,
                                    String customerName, BigDecimal amount,
                                    String invoiceNumber) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, "REMINDER",
                    String.format("Payment reminder ₹%s for %s", amount, customerName));
            return;
        }

        String message = String.format(
                "📋 Payment Reminder\n\nDear %s,\n\n" +
                        "This is a friendly reminder that *₹%s* is due against invoice *#%s*.\n\n" +
                        "Please clear the dues at your earliest convenience.\n\n" +
                        "_Thank you for your continued patronage._",
                customerName, amount, invoiceNumber
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "REMINDER", invoiceNumber);
    }

    // ── Send Birthday Offer ────────────────────────────────────────────
    @Async
    public void sendBirthdayOffer(Long tenantId, String customerPhone,
                                  String customerName, int discountPct) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, "BIRTHDAY",
                    String.format("Birthday offer %d%% for %s", discountPct, customerName));
            return;
        }

        String message = String.format(
                "🎂 Happy Birthday %s!\n\n" +
                        "Wishing you a wonderful birthday! 🎉\n\n" +
                        "As a special gift, enjoy *%d%% OFF* on your next purchase today!\n\n" +
                        "Just show this message at billing. Valid today only! 🎁",
                customerName, discountPct
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "BIRTHDAY", null);
    }

    // ── Send Broadcast Message ─────────────────────────────────────────
    @Async
    public void sendBroadcast(Long tenantId, List<String> phones,
                              String message, String templateName) {
        phones.forEach(phone -> {
            if (isPlaceholderConfig()) {
                logSimulated(tenantId, phone, "BROADCAST", message);
            } else {
                sendTextMessage(tenantId, phone, "", message, "BROADCAST", null);
            }
        });
    }

    // ── Send OTP ───────────────────────────────────────────────────────
    @Async
    public void sendOTP(String phone, String otp) {
        if (isPlaceholderConfig()) {
            log.info("[WHATSAPP SIM] OTP {} sent to {}", otp, phone);
            return;
        }

        String message = String.format(
                "🔐 Your BharatPOS OTP is: *%s*\n\nValid for 10 minutes. Do not share this code.",
                otp
        );

        sendTextMessage(null, phone, "", message, "OTP", null);
    }

    // ── Core send method ───────────────────────────────────────────────
    private void sendTextMessage(Long tenantId, String toPhone, String customerName,
                                 String messageText, String messageType, String referenceId) {
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");
        if (!cleanPhone.startsWith("91")) {
            cleanPhone = "91" + cleanPhone;
        }

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", cleanPhone,
                "type", "text",
                "text", Map.of("body", messageText)
        );

        WhatsAppMessage record = WhatsAppMessage.builder()
                .toPhone(toPhone)
                .customerName(customerName)
                .messageType(messageType)
                .messageBody(messageText)
                .status("pending")
                .referenceId(referenceId)
                .build();

        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(record::setTenant);
        }

        try {
            String response = webClient.post()
                    .uri(apiUrl + "/" + phoneNumberId + "/messages")
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            record.setStatus("sent");
            log.info("[WHATSAPP] Message sent to {} type {}", toPhone, messageType);
        } catch (Exception e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
            log.error("[WHATSAPP] Failed to send to {}: {}", toPhone, e.getMessage());
        }

        messageRepository.save(record);
    }

    private void logSimulated(Long tenantId, String phone, String type, String msg) {
        log.info("[WHATSAPP SIM] To:{} Type:{} Msg:{}", phone, type, msg);
        WhatsAppMessage record = WhatsAppMessage.builder()
                .toPhone(phone)
                .messageType(type)
                .messageBody(msg)
                .status("simulated")
                .build();
        if (tenantId != null) {
            tenantRepository.findById(tenantId).ifPresent(record::setTenant);
        }
        messageRepository.save(record);
    }

    private boolean isPlaceholderConfig() {
        return apiToken == null || apiToken.equals("placeholder") ||
                phoneNumberId == null || phoneNumberId.equals("placeholder");
    }
}