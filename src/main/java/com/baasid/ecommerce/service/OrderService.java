package com.baasid.ecommerce.service;

import com.baasid.ecommerce.dto.request.OrderRequest;
import com.baasid.ecommerce.dto.response.OrderResponse;
import com.baasid.ecommerce.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, Long userId);
    OrderResponse cancelOrder(UUID orderId, Long userId);
    OrderResponse updateStatus(UUID orderId, OrderStatus status);
    Page<OrderResponse> getOrders(Long userId, boolean isAdmin, Pageable pageable);
    OrderResponse getOrderById(UUID orderId, Long userId, boolean isAdmin);
}