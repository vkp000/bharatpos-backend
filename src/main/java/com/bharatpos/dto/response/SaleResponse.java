package com.bharatpos.dto.response;

import com.bharatpos.enums.PaymentMode;
import com.bharatpos.enums.SaleStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SaleResponse {
    private Long id;
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private PaymentMode paymentMode;
    private SaleStatus status;
    private String invoicePdfUrl;
    private Boolean whatsappSent;
    private LocalDateTime createdAt;
    private List<SaleItemResponse> items;

    @Data
    @Builder
    public static class SaleItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal gstRate;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
    }
}