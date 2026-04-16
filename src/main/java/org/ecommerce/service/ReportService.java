package org.ecommerce.service;

import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.dao.InventoryDAO;
import org.ecommerce.dao.OrderDAO;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * Generates summary statistics and formatted reports for the Reports tab.
 */
public class ReportService {

    private final OrderDAO     orderDAO     = new OrderDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    public record SalesSummary(
        BigDecimal totalRevenue,
        long       totalOrders,
        BigDecimal avgOrderValue,
        long       totalCustomers,
        int        lowStockCount
    ) {}

    // ── Summary card data ─────────────────────────────────────────────────────

    public SalesSummary getSalesSummary() throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(total_amount), 0) AS revenue,
                       COUNT(*)                        AS orders,
                       COALESCE(AVG(total_amount), 0)  AS avg_value
                FROM   orders
                WHERE  payment_status = 'paid'
                """;
        try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            long customers = countActiveCustomers();
            int  lowStock  = inventoryDAO.countLowStock();
            return new SalesSummary(
                rs.getBigDecimal("revenue"),
                rs.getLong("orders"),
                rs.getBigDecimal("avg_value"),
                customers,
                lowStock
            );
        }
    }

    private long countActiveCustomers() throws SQLException {
        try (Statement st = DatabaseConfig.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM users WHERE role = 'customer' AND is_active = TRUE")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ── Tabular report data (delegated to OrderDAO) ───────────────────────────

    public List<Object[]> getOrdersByStatus()         throws SQLException { return orderDAO.summaryByStatus(); }
    public List<Object[]> getMonthlyRevenue()         throws SQLException { return orderDAO.monthlyRevenue(); }
    public List<Object[]> getTopProducts(int limit)   throws SQLException { return orderDAO.topProductsByRevenue(limit); }

    // ── Formatted text report ─────────────────────────────────────────────────

    public String buildFullReport() throws SQLException {
        SalesSummary summary  = getSalesSummary();
        List<Object[]> top    = getTopProducts(5);
        List<Object[]> monthly = getMonthlyRevenue();
        List<Object[]> statuses = getOrdersByStatus();

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(80) + "\n");
        sb.append("  Smart E-Commerce System — Sales & Inventory Report\n");
        sb.append("=".repeat(80) + "\n\n");

        sb.append("── Overall Summary (paid orders) ──────────────────────\n");
        sb.append(String.format("  Total Revenue      : %,.2f%n", summary.totalRevenue()));
        sb.append(String.format("  Total Orders       : %d%n",     summary.totalOrders()));
        sb.append(String.format("  Avg Order Value    : %,.2f%n", summary.avgOrderValue()));
        sb.append(String.format("  Active Customers   : %d%n",     summary.totalCustomers()));
        sb.append(String.format("  Low Stock Products : %d%n",     summary.lowStockCount()));

        sb.append("\n── Orders by Status ───────────────────────────────────\n");
        sb.append(String.format("  %-15s %8s  %12s%n", "Status", "Count", "Revenue"));
        sb.append("  " + "─".repeat(40) + "\n");
        for (Object[] row : statuses)
            sb.append(String.format("  %-15s %8d  %,10.2f%n",
                row[0], row[1], row[2]));

        sb.append("\n── Top 5 Products by Revenue ──────────────────────────\n");
        sb.append(String.format("  %-30s %10s  %10s%n", "Product", "Revenue", "Units"));
        sb.append("  " + "─".repeat(55) + "\n");
        for (Object[] row : top)
            sb.append(String.format("  %-30s %,8.2f  %10d%n",
                truncate((String) row[0], 30), row[1], row[2]));

        sb.append("\n── Monthly Revenue (last 12 months) ───────────────────\n");
        sb.append(String.format("  %-10s %8s  %12s%n", "Month", "Orders", "Revenue"));
        sb.append("  " + "─".repeat(35) + "\n");
        for (Object[] row : monthly)
            sb.append(String.format("  %-10s %8d  %,10.2f%n",
                row[0], row[1], row[2]));

        sb.append("=".repeat(80) + "\n");
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
