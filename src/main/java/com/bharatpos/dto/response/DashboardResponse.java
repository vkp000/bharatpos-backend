package com.bharatpos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private BigDecimal todayRevenue;
    private Integer todayBills;
    private Integer todayItemsSold;
    private BigDecimal todayGST;
    private Integer lowStockCount;
    private BigDecimal pendingPayments;
    private BigDecimal weeklyRevenue;
    private BigDecimal monthlyRevenue;
    private List<TopProductDto> topProducts;
    private List<RecentBillDto> recentBills;
    private List<LowStockDto> lowStockItems;

    @Data
    @Builder
    public static class TopProductDto {
        private String name;
        private Integer unitsSold;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class RecentBillDto {
        private String invoiceNumber;
        private String customerName;
        private BigDecimal amount;
        private String paymentMode;
        private String status;
        private String timeAgo;
    }

    @Data
    @Builder
    public static class LowStockDto {
        private String productName;
        private Integer currentStock;
        private Integer reorderLevel;
        private String category;
    }
}