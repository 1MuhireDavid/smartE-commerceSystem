package org.ecommerce.api.repository;

import org.ecommerce.api.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    // Derived query — Spring Data generates the JOIN + WHERE from the method name
    Optional<InventoryEntity> findByProduct_ProductId(Long productId);

    // JPQL — compares two int fields on the same entity row
    @Query("SELECT i FROM InventoryEntity i WHERE i.qtyInStock <= i.reorderLevel")
    List<InventoryEntity> findLowStock();

    // Native SQL — atomic conditional decrement; returns rows affected (0 = insufficient stock)
    // clearAutomatically = true flushes the first-level cache so subsequent reads in the same
    // transaction see the updated qty_in_stock value.
    // last_updated is set in SQL because @PreUpdate does not fire for native DML statements.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE inventory
               SET qty_in_stock = qty_in_stock - :qty,
                   last_updated  = NOW()
             WHERE product_id   = :productId
               AND qty_in_stock >= :qty
            """,
            nativeQuery = true)
    int deductStock(@Param("productId") Long productId, @Param("qty") int qty);
}
