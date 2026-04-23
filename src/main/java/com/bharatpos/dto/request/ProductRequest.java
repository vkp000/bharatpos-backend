package com.bharatpos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    private String sku;
    private String barcode;
    private String category;
    private String unit;
    private String hsnCode;

    @NotNull
    private BigDecimal gstRate;

    @NotNull
    private BigDecimal costPrice;

    @NotNull
    private BigDecimal sellingPrice;

    private Integer reorderLevel;
    private Integer reorderQty;
    private Integer openingStock;
    private String supplierName;
    private String batchNumber;
    private String expiryDate;
}