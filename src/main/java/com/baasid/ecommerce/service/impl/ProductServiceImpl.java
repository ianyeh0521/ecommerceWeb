package com.baasid.ecommerce.service.impl;

import com.baasid.ecommerce.dto.request.ProductRequest;
import com.baasid.ecommerce.dto.response.ProductResponse;
import com.baasid.ecommerce.entity.Product;
import com.baasid.ecommerce.entity.ProductAuditLog;
import com.baasid.ecommerce.enums.OperationType;
import com.baasid.ecommerce.exception.NotFoundException;
import com.baasid.ecommerce.repository.AuditLogRepository;
import com.baasid.ecommerce.repository.ProductRepository;
import com.baasid.ecommerce.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setIsPublished(request.getIsPublished() != null && request.getIsPublished());
        product.setIsDeleted(false);
        Product saved = productRepository.save(product);

        writeAuditLog(saved.getId(), OperationType.CREATE, null, toJson(saved));

        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("找不到商品"));

        String beforeSnapshot = toJson(product);

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getIsPublished() != null) {
            product.setIsPublished(request.getIsPublished());
        }

        Product saved = productRepository.save(product);
        writeAuditLog(id, OperationType.UPDATE, beforeSnapshot, toJson(saved));

        return ProductResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("找不到商品"));

        String beforeSnapshot = toJson(product);

        product.setIsDeleted(true);
        productRepository.save(product);

        writeAuditLog(id, OperationType.DELETE, beforeSnapshot, null);
    }

    @Override
    public Page<ProductResponse> getProducts(Pageable pageable, boolean isAdmin) {
        if (isAdmin) {
            return productRepository.findAllByIsDeletedFalse(pageable).map(ProductResponse::from);
        }
        return productRepository.findAllByIsDeletedFalseAndIsPublishedTrue(pageable).map(ProductResponse::from);
    }

    @Override
    public ProductResponse getProduct(Long id, boolean isAdmin) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("找不到商品"));

        if (product.getIsDeleted()) {
            throw new NotFoundException("找不到商品");
        }
        if (!isAdmin && !product.getIsPublished()) {
            throw new NotFoundException("找不到商品");
        }

        return ProductResponse.from(product);
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            Object details = token.getDetails();
            if (details instanceof Long userId) {
                return userId;
            }
        }
        return null;
    }

    private String toJson(Product product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("快照序列化失敗", e);
        }
    }

    private void writeAuditLog(Long productId, OperationType type, String before, String after) {
        ProductAuditLog log = new ProductAuditLog();
        log.setProductId(productId);
        log.setOperatedBy(getCurrentUserId());
        log.setOperatedAt(LocalDateTime.now());
        log.setOperationType(type);
        log.setBeforeSnapshot(before);
        log.setAfterSnapshot(after);
        auditLogRepository.save(log);
    }
}
