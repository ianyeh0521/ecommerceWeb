package com.baasid.ecommerce.service.impl;

import com.baasid.ecommerce.dto.response.AuditLogResponse;
import com.baasid.ecommerce.entity.ProductAuditLog;
import com.baasid.ecommerce.enums.OperationType;
import com.baasid.ecommerce.repository.AuditLogRepository;
import com.baasid.ecommerce.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public Page<AuditLogResponse> getAuditLogs(Long productId, OperationType operationType,
                                                Long operatedBy, LocalDateTime from, LocalDateTime to,
                                                Pageable pageable) {
        Specification<ProductAuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (productId != null) predicates.add(cb.equal(root.get("productId"), productId));
            if (operationType != null) predicates.add(cb.equal(root.get("operationType"), operationType));
            if (operatedBy != null) predicates.add(cb.equal(root.get("operatedBy"), operatedBy));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("operatedAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("operatedAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from);
    }
}