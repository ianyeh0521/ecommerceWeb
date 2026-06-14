package com.baasid.ecommerce.repository;

import com.baasid.ecommerce.entity.ProductAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<ProductAuditLog, Long>, JpaSpecificationExecutor<ProductAuditLog> {
}
