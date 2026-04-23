package com.bharatpos.service;

import com.bharatpos.dto.response.DashboardResponse;
import com.bharatpos.repository.InventoryRepository;
import com.bharatpos.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SaleRepository saleRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long storeId) {
        LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        BigDecimal todayRevenue = saleRepository
                .sumRevenueByStoreAndDateRange(storeId, todayStart, todayEnd);
        Long todayBills = saleRepository
                .countSalesByStoreAndDateRange(storeId, todayStart, todayEnd);
        BigDecimal todayGST = saleRepository
                .sumTaxByStoreAndDateRange(storeId, todayStart, todayEnd);
        BigDecimal weeklyRevenue = saleRepository
                .sumRevenueByStoreAndDateRange(storeId, weekStart, todayEnd);
        BigDecimal monthlyRevenue = saleRepository
                .sumRevenueByStoreAndDateRange(storeId, monthStart, todayEnd);

        var lowStock = inventoryRepository.findLowStockByStore(storeId);
        var recentSales = saleRepository.findTop10ByStoreIdOrderByCreatedAtDesc(storeId);

        return DashboardResponse.builder()
                .todayRevenue(todayRevenue)
                .todayBills(todayBills.intValue())
                .todayItemsSold(0)
                .todayGST(todayGST)
                .weeklyRevenue(weeklyRevenue)
                .monthlyRevenue(monthlyRevenue)
                .lowStockCount(lowStock.size())
                .lowStockItems(lowStock.stream().map(inv ->
                        DashboardResponse.LowStockDto.builder()
                                .productName(inv.getProduct().getName())
                                .currentStock(inv.getQuantity())
                                .reorderLevel(inv.getProduct().getReorderLevel())
                                .category(inv.getProduct().getCategory())
                                .build()).toList())
                .recentBills(recentSales.stream().map(sale ->
                        DashboardResponse.RecentBillDto.builder()
                                .invoiceNumber(sale.getInvoiceNumber())
                                .customerName(
                                        sale.getCustomer() != null
                                                ? sale.getCustomer().getName()
                                                : "Walk-in")
                                .amount(sale.getGrandTotal())
                                .paymentMode(sale.getPaymentMode().name())
                                .status(sale.getStatus().name())
                                .timeAgo(getTimeAgo(sale.getCreatedAt()))
                                .build()).toList())
                .build();
    }

    private String getTimeAgo(java.time.LocalDateTime dateTime) {
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hr ago";
        return (hours / 24) + " days ago";
    }
}