package com.baasid.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    @Schema(description = "商品名稱", example = "Widget A")
    @NotBlank
    private String name;

    @Schema(description = "商品價格（≥ 0）", example = "9.99")
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @Schema(description = "庫存數量（≥ 0）", example = "100")
    @NotNull
    @Min(0)
    private Integer stock;

    @Schema(description = "是否上架", example = "false")
    private Boolean isPublished;
}
