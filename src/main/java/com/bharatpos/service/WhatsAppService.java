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
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
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

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return NumberFormat.getNumberInstance(new Locale("en", "IN"))
                .format(amount.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Async
    public void sendInvoice(Long tenantId, String customerPhone, String customerName,
                            String invoiceNumber, BigDecimal amount, String paymentMode) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, customerName, "INVOICE",
                    String.format("Invoice %s for ₹%s sent to %s",
                            invoiceNumber, formatAmount(amount), customerName));
            return;
        }

        String message = String.format(
                "Hello %s! 🛍️\n\nYour invoice *#%s* has been generated.\n\n" +
                        "💰 Amount: *₹%s*\n💳 Payment: *%s*\n\n" +
                        "Thank you for shopping with us! 🙏\n\n_Powered by BharatPOS_",
                customerName, invoiceNumber, formatAmount(amount), paymentMode
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "INVOICE", invoiceNumber);
    }

    @Async
    public void sendPaymentConfirmation(Long tenantId, String customerPhone,
                                        String customerName, String invoiceNumber,
                                        BigDecimal amount) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, customerName, "PAYMENT_CONFIRM",
                    String.format("Payment confirmed ₹%s for %s", formatAmount(amount), customerName));
            return;
        }

        String message = String.format(
                "✅ Payment Confirmed!\n\nHello %s,\n\n" +
                        "We've received your payment of *₹%s* for invoice *#%s*.\n\n" +
                        "Thank you for your business! Visit us again soon. 🙏",
                customerName, formatAmount(amount), invoiceNumber
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "PAYMENT_CONFIRM", invoiceNumber);
    }

    @Async
    public void sendLoyaltyUpdate(Long tenantId, String customerPhone,
                                  String customerName, int pointsEarned,
                                  int totalPoints) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, customerName, "LOYALTY",
                    String.format("Loyalty: +%d pts, total %d for %s",
                            pointsEarned, totalPoints, customerName));
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

    @Async
    public void sendPaymentReminder(Long tenantId, String customerPhone,
                                    String customerName, BigDecimal amount,
                                    String invoiceNumber) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, customerName, "REMINDER",
                    String.format("Payment reminder ₹%s for %s",
                            formatAmount(amount), customerName));
            return;
        }

        String message = String.format(
                "📋 Payment Reminder\n\nDear %s,\n\n" +
                        "This is a friendly reminder that *₹%s* is due against invoice *#%s*.\n\n" +
                        "Please clear the dues at your earliest convenience.\n\n" +
                        "_Thank you for your continued patronage._",
                customerName, formatAmount(amount), invoiceNumber
        );

        sendTextMessage(tenantId, customerPhone, customerName, message, "REMINDER", invoiceNumber);
    }

    @Async
    public void sendBirthdayOffer(Long tenantId, String customerPhone,
                                  String customerName, int discountPct) {
        if (isPlaceholderConfig()) {
            logSimulated(tenantId, customerPhone, customerName, "BIRTHDAY",
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

    @Async
    public void sendBroadcast(Long tenantId, List<String> phones,
                              String message, String templateName) {
        phones.forEach(phone ->
                logSimulated(tenantId, phone, "", "BROADCAST", message)
        );
        if (!isPlaceholderConfig()) {
            phones.forEach(phone ->
                    sendTextMessage(tenantId, phone, "", message, "BROADCAST", null)
            );
        }
    }

    @Async
    public void sendOTP(String phone, String otp) {
        if (isPlaceholderConfig()) {
            log.info("[WHATSAPP SIM] OTP {} would be sent to {}", otp, phone);
            return;
        }
        String message = String.format(
                "🔐 Your BharatPOS OTP is: *%s*\n\nValid for 10 minutes. Do not share.", otp);
        sendTextMessage(null, phone, "", message, "OTP", null);
    }

    private void sendTextMessage(Long tenantId, String toPhone, String customerName,
                                 String messageText, String messageType, String referenceId) {
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");
        if (!cleanPhone.startsWith("91")) {
            cleanPhone = "91" + cleanPhone;
        }

        Map<String, Object> textMap = new java.util.HashMap<>();
        textMap.put("body", messageText);

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", cleanPhone);
        payload.put("type", "text");
        payload.put("text", textMap);

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
            webClient.post()
                    .uri(apiUrl + "/" + phoneNumberId + "/messages")
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            record.setStatus("sent");
            log.info("[WHATSAPP] Sent to {} type {}", toPhone, messageType);
        } catch (Exception e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
            log.error("[WHATSAPP] Failed to {}: {}", toPhone, e.getMessage());
        }

        messageRepository.save(record);
    }

    private void logSimulated(Long tenantId, String phone, String customerName,
                              String type, String msg) {
        log.info("[WHATSAPP SIM] To:{} Type:{} Msg:{}", phone, type, msg);
        WhatsAppMessage record = WhatsAppMessage.builder()
                .toPhone(phone)
                .customerName(customerName)
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
        return apiToken == null ||
                apiToken.equals("placeholder") ||
                phoneNumberId == null ||
                phoneNumberId.equals("placeholder");
    }
}