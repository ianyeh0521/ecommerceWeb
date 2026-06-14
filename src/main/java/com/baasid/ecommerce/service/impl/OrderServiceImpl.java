package com.baasid.ecommerce.service.impl;

import com.baasid.ecommerce.dto.request.OrderItemRequest;
import com.baasid.ecommerce.dto.request.OrderRequest;
import com.baasid.ecommerce.dto.response.OrderItemResponse;
import com.baasid.ecommerce.dto.response.OrderResponse;
import com.baasid.ecommerce.entity.Order;
import com.baasid.ecommerce.entity.OrderItem;
import com.baasid.ecommerce.entity.Product;
import com.baasid.ecommerce.enums.OrderStatus;
import com.baasid.ecommerce.exception.BadRequestException;
import com.baasid.ecommerce.exception.NotFoundException;
import com.baasid.ecommerce.repository.OrderItemRepository;
import com.baasid.ecommerce.repository.OrderRepository;
import com.baasid.ecommerce.repository.ProductRepository;
import com.baasid.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        List<OrderItemRequest> items = request.getItems();

        // Step 1: 避免重複下單
        Set<Long> seen = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!seen.add(item.getProductId())) {
                throw new BadRequestException("訂單內不可重複下相同商品");
            }
        }

        // Step 2: 檢查商品是否存在及庫存
        List<Product> products = new ArrayList<>();
        for (OrderItemRequest item : items) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null || product.getIsDeleted() || !product.getIsPublished()) {
                throw new BadRequestException("商品不存在或已下架");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BadRequestException("庫存不足");
            }
            products.add(product);
        }

        // Step 3: 計算總金額
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            Product product = products.get(i);
            int qty = items.get(i).getQuantity();
            product.setStock(product.getStock() - qty);
            productRepository.save(product);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        // Step 4: 建立訂單
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Step 5: 建立訂單明細
        List<OrderItem> savedItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(items.get(i).getProductId());
            orderItem.setQuantity(items.get(i).getQuantity());
            orderItem.setUnitPrice(products.get(i).getPrice());
            savedItems.add(orderItemRepository.save(orderItem));
        }

        return buildResponse(savedOrder, savedItems, products);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new NotFoundException("找不到訂單");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("只有待處理的訂單可以取消");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<Product> products = new ArrayList<>();
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("找不到商品"));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
            products.add(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return buildResponse(saved, items, products);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus status) {
        if (status == OrderStatus.PENDING) {
            throw new BadRequestException("無效的訂單狀態");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("找不到訂單"));
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<Product> products = loadProducts(items);
        return buildResponse(saved, items, products);
    }

    @Override
    public List<OrderResponse> getOrders(Long userId, boolean isAdmin) {
        List<Order> orders;
        if (isAdmin) {
            orders = orderRepository.findAll();
        } else {
            orders = orderRepository.findByUserId(userId);
        }

        List<OrderResponse> result = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<Product> products = loadProducts(items);
            result.add(buildResponse(order, items, products));
        }
        return result;
    }

    @Override
    public OrderResponse getOrderById(UUID orderId, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new NotFoundException("找不到訂單");
        }
        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new NotFoundException("找不到訂單");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<Product> products = loadProducts(items);
        return buildResponse(order, items, products);
    }

    private List<Product> loadProducts(List<OrderItem> items) {
        List<Long> productIds = new ArrayList<>();
        for (OrderItem item : items) {
            productIds.add(item.getProductId());
        }
        List<Product> fetched = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product p : fetched) {
            productMap.put(p.getId(), p);
        }
        List<Product> ordered = new ArrayList<>();
        for (OrderItem item : items) {
            ordered.add(productMap.get(item.getProductId()));
        }
        return ordered;
    }

    private OrderResponse buildResponse(Order order, List<OrderItem> items, List<Product> products) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            Product product = products.get(i);
            OrderItemResponse ir = new OrderItemResponse();
            ir.setProductId(item.getProductId());
            ir.setProductName(product != null ? product.getName() : null);
            ir.setQuantity(item.getQuantity());
            ir.setUnitPrice(item.getUnitPrice());
            itemResponses.add(ir);
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus().name());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(itemResponses);
        return response;
    }
}