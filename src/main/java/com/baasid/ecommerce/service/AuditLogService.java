package com.baasid.ecommerce.service;

import com.baasid.ecommerce.dto.response.AuditLogResponse;
import com.baasid.ecommerce.enums.OperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    Page<AuditLogResponse> getAuditLogs(Long productId, OperationType operationType,
                                         Long operatedBy, LocalDateTime from, LocalDateTime to,
                                         Pageable pageable);
}