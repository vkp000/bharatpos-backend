package com.bharatpos.dto.request;

import com.bharatpos.enums.PaymentMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateSaleRequest {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    private Long customerId;

    @NotEmpty(message = "Cart cannot be empty")
    private List<CartItemRequest> items;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String paymentReference;
    private BigDecimal discountPercent;
    private Boolean sendWhatsApp;

    @Data
    public static class CartItemRequest {
        @NotNull
        private Long productId;

        @NotNull
        private Integer quantity;

        @NotNull
        private BigDecimal unitPrice;
    }
}