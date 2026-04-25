package org.ecommerce.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderStatsDto {

    private final List<StatusCount> byStatus;
    private final BigDecimal        paidRevenue;

    public OrderStatsDto(List<StatusCount> byStatus, BigDecimal paidRevenue) {
        this.byStatus    = byStatus;
        this.paidRevenue = paidRevenue;
    }

    public List<StatusCount> getByStatus()   { return byStatus; }
    public BigDecimal        getPaidRevenue() { return paidRevenue; }

    public static final class StatusCount {

        private final String     status;
        private final long       orderCount;
        private final BigDecimal revenue;

        public StatusCount(String status, long orderCount, BigDecimal revenue) {
            this.status     = status;
            this.orderCount = orderCount;
            this.revenue    = revenue;
        }

        public String     getStatus()     { return status; }
        public long       getOrderCount() { return orderCount; }
        public BigDecimal getRevenue()    { return revenue; }
    }
}
