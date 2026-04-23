package com.bharatpos.service;

import com.bharatpos.dto.request.CreateSaleRequest;
import com.bharatpos.dto.response.SaleResponse;
import com.bharatpos.entity.*;
import com.bharatpos.enums.SaleStatus;
import com.bharatpos.exception.BadRequestException;
import com.bharatpos.exception.ResourceNotFoundException;
import com.bharatpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    private static final AtomicInteger invoiceCounter = new AtomicInteger(1000);

    @Transactional
    public SaleResponse createSale(Long tenantId, Long userId, CreateSaleRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));

        User cashier = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElse(null);
        }

        // Build sale items and calculate totals
        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        int totalQty = 0;

        for (CreateSaleRequest.CartItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            // Check and decrement stock
            Inventory inventory = inventoryRepository
                    .findByProductIdAndStoreId(product.getId(), store.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "No inventory record for product: " + product.getName()));

            if (inventory.getQuantity() < itemReq.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + product.getName()
                        + ". Available: " + inventory.getQuantity());
            }

            inventoryRepository.decrementStock(product.getId(), store.getId(), itemReq.getQuantity());

            // Calculate GST (MRP-inclusive pricing)
            BigDecimal lineTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal gstRate = product.getGstRate();
            BigDecimal taxAmount = BigDecimal.ZERO;

            if (gstRate != null && gstRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxFactor = gstRate.divide(BigDecimal.valueOf(100).add(gstRate), 10, RoundingMode.HALF_UP);
                taxAmount = lineTotal.multiply(taxFactor).setScale(2, RoundingMode.HALF_UP);
            }

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .gstRate(gstRate != null ? gstRate : BigDecimal.ZERO)
                    .taxAmount(taxAmount)
                    .lineTotal(lineTotal)
                    .build();

            saleItems.add(saleItem);
            subtotal = subtotal.add(lineTotal);
            totalTax = totalTax.add(taxAmount);
            totalQty += itemReq.getQuantity();
        }

        // Apply discount
        BigDecimal discountPct = request.getDiscountPercent() != null ? request.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountAmt = subtotal.multiply(discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.subtract(discountAmt);

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber(store);

        Sale sale = Sale.builder()
                .store(store)
                .cashier(cashier)
                .customer(customer)
                .invoiceNumber(invoiceNumber)
                .subtotal(subtotal)
                .discountPercent(discountPct)
                .discountAmount(discountAmt)
                .taxAmount(totalTax)
                .grandTotal(grandTotal)
                .paymentMode(request.getPaymentMode())
                .paymentReference(request.getPaymentReference())
                .status(SaleStatus.COMPLETED)
                .whatsappSent(false)
                .build();

        sale = saleRepository.save(sale);

        // Save sale items with sale reference
        Sale finalSale = sale;
        saleItems.forEach(item -> item.setSale(finalSale));
        saleItemRepository.saveAll(saleItems);

        // Update customer stats
        if (customer != null) {
            customer.setTotalSpend(customer.getTotalSpend().add(grandTotal));
            customer.setTotalVisits(customer.getTotalVisits() + 1);
            customer.setLastVisit(LocalDateTime.now());
            int pointsEarned = grandTotal.divide(BigDecimal.TEN, 0, RoundingMode.FLOOR).intValue();
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);
            customerRepository.save(customer);
        }

        log.info("Sale created: {} | Total: {} | Store: {}", invoiceNumber, grandTotal, store.getName());

        return mapToResponse(sale, saleItems);
    }

    public List<Sale> getRecentSales(Long storeId) {
        return saleRepository.findTop10ByStoreIdOrderByCreatedAtDesc(storeId);
    }

    private String generateInvoiceNumber(Store store) {
        String prefix = store.getStoreCode().substring(0, Math.min(3, store.getStoreCode().length())).toUpperCase();
        int year = LocalDateTime.now().getYear();
        int seq = invoiceCounter.incrementAndGet();
        String candidate = String.format("INV-%s-%d-%05d", prefix, year, seq);
        while (saleRepository.existsByInvoiceNumber(candidate)) {
            seq = invoiceCounter.incrementAndGet();
            candidate = String.format("INV-%s-%d-%05d", prefix, year, seq);
        }
        return candidate;
    }

    private SaleResponse mapToResponse(Sale sale, List<SaleItem> items) {
        return SaleResponse.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .customerName(sale.getCustomer() != null ? sale.getCustomer().getName() : "Walk-in")
                .customerPhone(sale.getCustomer() != null ? sale.getCustomer().getPhone() : null)
                .subtotal(sale.getSubtotal())
                .discountAmount(sale.getDiscountAmount())
                .taxAmount(sale.getTaxAmount())
                .grandTotal(sale.getGrandTotal())
                .paymentMode(sale.getPaymentMode())
                .status(sale.getStatus())
                .whatsappSent(sale.getWhatsappSent())
                .createdAt(sale.getCreatedAt())
                .items(items.stream().map(item -> SaleResponse.SaleItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .gstRate(item.getGstRate())
                        .taxAmount(item.getTaxAmount())
                        .lineTotal(item.getLineTotal())
                        .build()).toList())
                .build();
    }
}