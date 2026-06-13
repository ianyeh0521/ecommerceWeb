package com.baasid.ecommerce.controller;

import com.baasid.ecommerce.dto.response.AuditLogResponse;
import com.baasid.ecommerce.enums.OperationType;
import com.baasid.ecommerce.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Logs", description = "操作日誌查詢")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Operation(summary = "查詢商品操作日誌，僅限 ADMIN")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) OperationType operationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLogResponse> result = auditLogRepository
                .findWithFilters(productId, operationType, PageRequest.of(page, size))
                .map(AuditLogResponse::from);
        return ResponseEntity.ok(result);
    }
}
