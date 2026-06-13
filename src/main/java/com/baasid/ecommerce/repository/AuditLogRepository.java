package com.baasid.ecommerce.repository;

import com.baasid.ecommerce.entity.ProductAuditLog;
import com.baasid.ecommerce.enums.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<ProductAuditLog, Long> {

    @Query("SELECT a FROM ProductAuditLog a " +
           "WHERE (:productId IS NULL OR a.productId = :productId) " +
           "AND (:operationType IS NULL OR a.operationType = :operationType) " +
           "ORDER BY a.operatedAt DESC")
    Page<ProductAuditLog> findWithFilters(
            @Param("productId") Long productId,
            @Param("operationType") OperationType operationType,
            Pageable pageable);
}
