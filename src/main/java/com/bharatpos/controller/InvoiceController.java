package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Sale;
import com.bharatpos.entity.SaleItem;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.SaleItemRepository;
import com.bharatpos.repository.SaleRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    @GetMapping("/{invoiceNumber}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoice(
            @PathVariable String invoiceNumber) {

        Sale sale = saleRepository.findAll().stream()
                .filter(s -> s.getInvoiceNumber().equals(invoiceNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found: " + invoiceNumber));

        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());

        Map<String, Object> invoice = buildInvoiceData(sale, items);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/sale/{saleId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoiceBySaleId(
            @PathVariable Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        return ResponseEntity.ok(ApiResponse.success(buildInvoiceData(sale, items)));
    }

    private Map<String, Object> buildInvoiceData(Sale sale, List<SaleItem> items) {
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("invoiceNumber", sale.getInvoiceNumber());
        invoice.put("date", sale.getCreatedAt() != null
                ? sale.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                : "—");
        invoice.put("customerName", sale.getCustomer() != null
                ? sale.getCustomer().getName() : "Walk-in Customer");
        invoice.put("customerPhone", sale.getCustomer() != null
                ? sale.getCustomer().getPhone() : null);
        invoice.put("customerGstin", sale.getCustomer() != null
                ? sale.getCustomer().getGstin() : null);
        invoice.put("subtotal", sale.getSubtotal());
        invoice.put("discountAmount", sale.getDiscountAmount());
        invoice.put("discountPercent", sale.getDiscountPercent());
        invoice.put("taxAmount", sale.getTaxAmount());
        invoice.put("grandTotal", sale.getGrandTotal());
        invoice.put("paymentMode", sale.getPaymentMode());
        invoice.put("status", sale.getStatus());
        invoice.put("storeName", sale.getStore() != null
                ? sale.getStore().getName() : "BharatPOS Store");
        invoice.put("storeAddress", sale.getStore() != null
                ? sale.getStore().getAddress() : null);
        invoice.put("storeGstin", sale.getStore() != null
                ? sale.getStore().getGstin() : null);
        invoice.put("cgst", sale.getTaxAmount() != null
                ? sale.getTaxAmount().divide(BigDecimal.valueOf(2), 2,
                java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        invoice.put("sgst", sale.getTaxAmount() != null
                ? sale.getTaxAmount().divide(BigDecimal.valueOf(2), 2,
                java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
        invoice.put("items", items.stream().map(item -> {
            Map<String, Object> i = new HashMap<>();
            i.put("name", item.getProduct() != null ? item.getProduct().getName() : "Product");
            i.put("hsnCode", item.getProduct() != null ? item.getProduct().getHsnCode() : null);
            i.put("quantity", item.getQuantity());
            i.put("unitPrice", item.getUnitPrice());
            i.put("gstRate", item.getGstRate());
            i.put("taxAmount", item.getTaxAmount());
            i.put("lineTotal", item.getLineTotal());
            return i;
        }).collect(Collectors.toList()));
        return invoice;
    }
}