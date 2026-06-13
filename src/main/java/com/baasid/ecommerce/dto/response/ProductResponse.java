package com.baasid.ecommerce.dto.response;

import com.baasid.ecommerce.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {

    @Schema(description = "商品 ID")
    private Long id;

    @Schema(description = "商品名稱")
    private String name;

    @Schema(description = "商品價格")
    private BigDecimal price;

    @Schema(description = "庫存數量")
    private Integer stock;

    @Schema(description = "是否上架")
    private Boolean isPublished;

    @Schema(description = "是否已刪除（軟刪除）")
    private Boolean isDeleted;

    @Schema(description = "建立時間")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間")
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        ProductResponse response = new ProductResponse();
        response.id = product.getId();
        response.name = product.getName();
        response.price = product.getPrice();
        response.stock = product.getStock();
        response.isPublished = product.getIsPublished();
        response.isDeleted = product.getIsDeleted();
        response.createdAt = product.getCreatedAt();
        response.updatedAt = product.getUpdatedAt();
        return response;
    }
}
