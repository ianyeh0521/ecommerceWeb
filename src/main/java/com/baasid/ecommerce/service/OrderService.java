package com.baasid.ecommerce.service;

import com.baasid.ecommerce.dto.request.OrderRequest;
import com.baasid.ecommerce.dto.response.OrderResponse;
import com.baasid.ecommerce.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, Long userId);
    OrderResponse cancelOrder(UUID orderId, Long userId);
    OrderResponse updateStatus(UUID orderId, OrderStatus status);
    List<OrderResponse> getOrders(Long userId, boolean isAdmin);
    OrderResponse getOrderById(UUID orderId, Long userId, boolean isAdmin);
}