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

        BigDecimal todayRevenue = orZero(
                saleRepository.sumRevenueByStoreAndDateRange(storeId, todayStart, todayEnd));
        Long todayBills = orZeroLong(
                saleRepository.countSalesByStoreAndDateRange(storeId, todayStart, todayEnd));
        BigDecimal todayGST = orZero(
                saleRepository.sumTaxByStoreAndDateRange(storeId, todayStart, todayEnd));
        BigDecimal weeklyRevenue = orZero(
                saleRepository.sumRevenueByStoreAndDateRange(storeId, weekStart, todayEnd));
        BigDecimal monthlyRevenue = orZero(
                saleRepository.sumRevenueByStoreAndDateRange(storeId, monthStart, todayEnd));

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
                                .reorderLevel(inv.getProduct().getReorderLevel() != null
                                        ? inv.getProduct().getReorderLevel() : 0)
                                .category(inv.getProduct().getCategory())
                                .build()).toList())
                .recentBills(recentSales.stream().map(sale ->
                        DashboardResponse.RecentBillDto.builder()
                                .invoiceNumber(sale.getInvoiceNumber())
                                .customerName(sale.getCustomer() != null
                                        ? sale.getCustomer().getName() : "Walk-in")
                                .amount(sale.getGrandTotal())
                                .paymentMode(sale.getPaymentMode().name())
                                .status(sale.getStatus().name())
                                .timeAgo(getTimeAgo(sale.getCreatedAt()))
                                .build()).toList())
                .build();
    }

    private BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private Long orZeroLong(Long val) {
        return val != null ? val : 0L;
    }

    private String getTimeAgo(LocalDateTime dt) {
        if (dt == null) return "—";
        long mins = ChronoUnit.MINUTES.between(dt, LocalDateTime.now());
        if (mins < 1) return "Just now";
        if (mins < 60) return mins + " min ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + " hr ago";
        return (hrs / 24) + " days ago";
    }
}