package org.ecommerce.api.repository;

import org.ecommerce.api.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // JOIN FETCH on user avoids N+1 lazy-load hits when the caller accesses order.user.
    // The explicit countQuery is required because JOIN FETCH prevents Spring Data from
    // deriving a correct count automatically.
    @Query(value = """
            SELECT o FROM OrderEntity o
            JOIN FETCH o.user
            WHERE (:userId IS NULL OR o.userId = :userId)
              AND (:status IS NULL OR o.status  = :status)
            """,
            countQuery = """
            SELECT COUNT(o) FROM OrderEntity o
            WHERE (:userId IS NULL OR o.userId = :userId)
              AND (:status IS NULL OR o.status  = :status)
            """)
    Page<OrderEntity> search(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable);

    // JPQL aggregate: order count and revenue per status — used for reporting.
    // Returns Object[] rows where [0]=status, [1]=count (Long), [2]=revenue (BigDecimal|null).
    @Query("""
            SELECT o.status, COUNT(o), SUM(o.totalAmount)
            FROM   OrderEntity o
            GROUP  BY o.status
            ORDER  BY o.status
            """)
    List<Object[]> getStatsByStatus();

    // Native SQL: total revenue from orders whose payment has been confirmed as paid.
    // COALESCE handles the empty-table case; native is used because JPQL's COALESCE with
    // a numeric literal can produce type-mismatch issues with BigDecimal aggregates.
    @Query(value = """
            SELECT COALESCE(SUM(total_amount), 0)
              FROM orders
             WHERE payment_status = 'paid'
            """,
            nativeQuery = true)
    BigDecimal sumPaidRevenue();
}
