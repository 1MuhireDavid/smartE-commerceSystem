package org.ecommerce.api.repository;

import org.ecommerce.api.entity.ActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {

    @Query("""
        SELECT a FROM ActivityLogEntity a
        WHERE (:userId    IS NULL OR a.userId     = :userId)
          AND (:eventType IS NULL OR a.eventType  = :eventType)
        """)
    Page<ActivityLogEntity> search(
            @Param("userId")    Long userId,
            @Param("eventType") String eventType,
            Pageable pageable);
}
