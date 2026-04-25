package com.bharatpos.controller;

import com.bharatpos.dto.response.ApiResponse;
import com.bharatpos.entity.Sale;
import com.bharatpos.repository.SaleRepository;
import com.bharatpos.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/gst")
@RequiredArgsConstructor
public class GSTController {

    private final SaleRepository saleRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {

        Long storeId = SecurityUtils.getCurrentStoreId();
        LocalDateTime now = LocalDateTime.now();
        int m = month == 0 ? now.getMonthValue() : month;
        int y = year == 0 ? now.getYear() : year;

        LocalDateTime start = LocalDateTime.of(y, m, 1, 0, 0);
        LocalDateTime end = start.plusMonths(1);

        List<Sale> sales = saleRepository.findByStoreIdAndDateRange(storeId, start, end);

        BigDecimal totalTaxable = sales.stream()
                .map(s -> s.getGrandTotal().subtract(s.getTaxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTax = sales.stream()
                .map(Sale::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cgst = totalTax.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal sgst = cgst;

        long b2b = sales.stream().filter(s -> s.getCustomer() != null
                && s.getCustomer().getGstin() != null
                && !s.getCustomer().getGstin().isBlank()).count();
        long b2c = sales.size() - b2b;

        List<Map<String, Object>> invoiceList = sales.stream().map(s -> {
            Map<String, Object> inv = new HashMap<>();
            inv.put("id", s.getId());
            inv.put("invoiceNumber", s.getInvoiceNumber());
            inv.put("date", s.getCreatedAt());
            inv.put("customerName", s.getCustomer() != null ? s.getCustomer().getName() : "Walk-in");
            inv.put("customerGstin", s.getCustomer() != null ? s.getCustomer().getGstin() : null);
            inv.put("type", (s.getCustomer() != null && s.getCustomer().getGstin() != null
                    && !s.getCustomer().getGstin().isBlank()) ? "B2B" : "B2C");
            inv.put("grandTotal", s.getGrandTotal());
            inv.put("taxAmount", s.getTaxAmount());
            inv.put("taxable", s.getGrandTotal().subtract(s.getTaxAmount()));
            inv.put("cgst", s.getTaxAmount().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
            inv.put("sgst", s.getTaxAmount().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
            inv.put("igst", BigDecimal.ZERO);
            inv.put("status", s.getStatus());
            return inv;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalInvoices", sales.size());
        result.put("b2bInvoices", b2b);
        result.put("b2cInvoices", b2c);
        result.put("totalTaxable", totalTaxable);
        result.put("totalCGST", cgst);
        result.put("totalSGST", sgst);
        result.put("totalIGST", BigDecimal.ZERO);
        result.put("totalTax", totalTax);
        result.put("invoices", invoiceList);
        result.put("month", m);
        result.put("year", y);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/gstr1")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGSTR1(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return getSummary(month, year);
    }
}