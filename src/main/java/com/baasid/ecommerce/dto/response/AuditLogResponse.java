package com.baasid.ecommerce.dto.response;

import com.baasid.ecommerce.entity.ProductAuditLog;
import com.baasid.ecommerce.enums.OperationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long productId;
    private Long operatedBy;
    private LocalDateTime operatedAt;
    private OperationType operationType;
    private String beforeSnapshot;
    private String afterSnapshot;

    public static AuditLogResponse from(ProductAuditLog log) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.id = log.getId();
        dto.productId = log.getProductId();
        dto.operatedBy = log.getOperatedBy();
        dto.operatedAt = log.getOperatedAt();
        dto.operationType = log.getOperationType();
        dto.beforeSnapshot = log.getBeforeSnapshot();
        dto.afterSnapshot = log.getAfterSnapshot();
        return dto;
    }
}
