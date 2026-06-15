package com.baasid.ecommerce.controller;

import com.baasid.ecommerce.dto.request.OrderRequest;
import com.baasid.ecommerce.dto.response.OrderResponse;
import com.baasid.ecommerce.enums.OrderStatus;
import com.baasid.ecommerce.exception.BadRequestException;
import com.baasid.ecommerce.security.SecurityUtils;
import com.baasid.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Orders", description = "訂單管理")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "建立訂單，僅限 USER")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request,
                                                     Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, userId));
    }

    @Operation(summary = "查詢訂單列表；USER 查自己的，ADMIN 查全部")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(orderService.getOrders(userId, isAdmin, pageable));
    }

    @Operation(summary = "查詢單一訂單；USER 只能查自己的")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id,
                                                      Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(orderService.getOrderById(id, userId, isAdmin));
    }

    @Operation(summary = "取消訂單，僅 PENDING 狀態，僅限 USER")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id,
                                                     Authentication authentication) {
        Long userId = SecurityUtils.getUserId(authentication);
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }

    @Operation(summary = "更新訂單狀態，僅限 ADMIN")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable UUID id,
                                                      @RequestBody Map<String, String> body) {
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(body.get("status"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("無效的訂單狀態");
        }
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}
