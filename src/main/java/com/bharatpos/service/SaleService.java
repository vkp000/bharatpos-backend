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
    private final WhatsAppService whatsAppService;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public SaleResponse createSale(Long tenantId, Long userId, CreateSaleRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));

        User cashier = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
        }

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateSaleRequest.CartItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            var inventoryOpt = inventoryRepository.findByProductIdAndStoreId(
                    product.getId(), store.getId());

            if (inventoryOpt.isPresent()) {
                Inventory inv = inventoryOpt.get();
                if (inv.getQuantity() < itemReq.getQuantity()) {
                    throw new BadRequestException("Insufficient stock for: " + product.getName()
                            + ". Available: " + inv.getQuantity());
                }
                inventoryRepository.decrementStock(
                        product.getId(), store.getId(), itemReq.getQuantity());
            }

            BigDecimal lineTotal = itemReq.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal gstRate = product.getGstRate() != null ? product.getGstRate() : BigDecimal.ZERO;
            BigDecimal taxAmount = BigDecimal.ZERO;

            if (gstRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxFactor = gstRate.divide(
                        BigDecimal.valueOf(100).add(gstRate), 10, RoundingMode.HALF_UP);
                taxAmount = lineTotal.multiply(taxFactor).setScale(2, RoundingMode.HALF_UP);
            }

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .gstRate(gstRate)
                    .taxAmount(taxAmount)
                    .lineTotal(lineTotal)
                    .build();

            saleItems.add(saleItem);
            subtotal = subtotal.add(lineTotal);
            totalTax = totalTax.add(taxAmount);
        }

        BigDecimal discountPct = request.getDiscountPercent() != null
                ? request.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountAmt = subtotal.multiply(discountPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.subtract(discountAmt);

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
            updateSegment(customer);
            customerRepository.save(customer);

            // Auto WhatsApp
            if (customer.getPhone() != null && !customer.getPhone().isBlank()
                    && Boolean.TRUE.equals(request.getSendWhatsApp())) {
                final String phone = customer.getPhone();
                final String name = customer.getName();
                final String inv = invoiceNumber;
                final BigDecimal total = grandTotal;
                final int pts = pointsEarned;
                final int loyaltyBalance = customer.getLoyaltyPoints();

                whatsAppService.sendInvoice(tenantId, phone, name, inv, total,
                        request.getPaymentMode().name());

                if (pts > 0) {
                    whatsAppService.sendLoyaltyUpdate(tenantId, phone, name, pts, loyaltyBalance);
                }

                sale.setWhatsappSent(true);
                sale = saleRepository.save(sale);
            }
        }

        // Increment subscription invoice count
        try {
            subscriptionRepository.incrementInvoiceCount(tenantId);
        } catch (Exception e) {
            log.warn("Could not increment invoice count: {}", e.getMessage());
        }

        log.info("Sale created: {} | ₹{} | Store: {}", invoiceNumber, grandTotal, store.getName());
        return buildResponse(sale, saleItems, customer);
    }

    private void updateSegment(Customer customer) {
        int visits = customer.getTotalVisits();
        BigDecimal spend = customer.getTotalSpend();
        if (visits >= 10 && spend.compareTo(BigDecimal.valueOf(10000)) > 0) {
            customer.setSegment("champion");
        } else if (visits >= 5) {
            customer.setSegment("loyal");
        } else {
            customer.setSegment("new");
        }
    }

    private String generateInvoiceNumber(Store store) {
        String prefix = "INV";
        int year = LocalDateTime.now().getYear();
        long count = saleRepository.count() + 1;
        String candidate = String.format("%s-%d-%05d", prefix, year, count);
        int attempts = 0;
        while (saleRepository.existsByInvoiceNumber(candidate) && attempts < 100) {
            count++;
            attempts++;
            candidate = String.format("%s-%d-%05d", prefix, year, count);
        }
        return candidate;
    }

    private SaleResponse buildResponse(Sale sale, List<SaleItem> items, Customer customer) {
        return SaleResponse.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .customerName(customer != null ? customer.getName() : "Walk-in")
                .customerPhone(customer != null ? customer.getPhone() : null)
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